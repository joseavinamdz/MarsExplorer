package mars;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;

import java.util.Random;

public class Exolorer extends Agent {

    private final Random random = new Random();
    public boolean hasSample = false;
    private Double motherShipDirection;
    private Double motherShipDistance;
    private Map.Direction prev;
    private AID[] motherShip;

    private Agent self = this;


    public void move(Map.Direction direction) {
        while (goingBack(direction)) {
            direction = Map.Direction.values()[random.nextInt(Map.Direction.values().length)];
        }
        ExplorerInfo explorerInfo = Map.move(getAID().getName(), direction);
        motherShipDistance = explorerInfo.distance;
        motherShipDirection = explorerInfo.angle;
        if (!hasSample && explorerInfo.foundMineral) {
            hasSample = true;
            Map.collectSample(getAID().getName());
            Logger.log(getAID().getLocalName() + " : zmalazł minerał");
        }
        prev = direction;
    }

    public boolean goingBack(Map.Direction direction) {
        if (prev != null) {
            int dirIndex = Map.Direction.valueOf(direction.toString()).ordinal();
            int prevIndex = Map.Direction.valueOf(prev.toString()).ordinal();
            return dirIndex == 0 && prevIndex == 2 || dirIndex == 1 && prevIndex == 3 || dirIndex == 2 && prevIndex == 1 || dirIndex == 3 && prevIndex == 1;
        } else return false;

    }

    public Map.Direction goToMothership() {
        Map.Direction direction = null;
        if (motherShipDirection != null) {
            if (motherShipDirection >= 315 || motherShipDirection < 45) {
                direction = Map.Direction.S;
            } else if (motherShipDirection >= 45 && motherShipDirection < 135) {
                direction = Map.Direction.W;
            } else if (motherShipDirection >= 135 && motherShipDirection < 225) {
                direction = Map.Direction.N;
            } else if (motherShipDirection >= 225 && motherShipDirection < 315) {
                direction = Map.Direction.E;
            }
        }
        return direction;
    }

    @Override
    protected void setup() {
        System.out.printf("Hello world, from %s\n", getAID().getLocalName());

        addBehaviour(new OneShotBehaviour() {
            @Override
            public void action() {
                DFAgentDescription template = new DFAgentDescription();
                ServiceDescription sd = new ServiceDescription();
                sd.setType("book-selling");
                template.addServices(sd);
                try {
                    DFAgentDescription[] result = DFService.search(self, template);
                    System.out.println("Found the following seller agents:");
                    motherShip = new AID[result.length];
                    for (int i = 0; i < result.length; ++i) {
                        motherShip[i] = result[i].getName();
                        System.out.println(motherShip[i].getName());
                    }
                } catch (FIPAException fe) {
                    fe.printStackTrace();
                }
            }
        });
        addBehaviour(new TickerBehaviour(this, 200) {
            protected void onTick() {
                if (hasSample && motherShipDistance == 0) {
                    hasSample = false;
                    move(Map.Direction.values()[random.nextInt(Map.Direction.values().length)]);
                    Logger.log(getAID().getLocalName() + " : wrócił do bazy");

                    ACLMessage order = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                    order.addReceiver(motherShip[0]);
                    order.setContent("mam minerał");
                    order.setConversationId("sample-delivery");
                    order.setReplyWith("delivery" + System.currentTimeMillis());
                    myAgent.send(order);


                } else if (hasSample && motherShipDistance > 0) {
                    move(goToMothership());
                    Logger.log(getAID().getLocalName() + " : wraca do bazy z minerałem");
                } else {
                    move(Map.Direction.values()[random.nextInt(Map.Direction.values().length)]);
                    Logger.log(getAID().getLocalName() + " : szuka...");
                }
            }
        });


    }

    public void runStep(){
        addBehaviour(new Step());
    }

    @Override
    protected void takeDown() {
        System.out.printf("Taking down agent, from %s\n", getAID().getName());
    }

    class Step extends OneShotBehaviour {

        @Override
        public void action() {
            if (hasSample && motherShipDistance == 0) {
                hasSample = false;
                move(Map.Direction.values()[random.nextInt(Map.Direction.values().length)]);
                Logger.log(getAID().getLocalName() + " : wrócił do bazy");
            } else if (hasSample && motherShipDistance > 0) {
                move(goToMothership());
                Logger.log(getAID().getLocalName() + " : wracam do bazy z minerałem");
            } else {
                move(Map.Direction.values()[random.nextInt(Map.Direction.values().length)]);
                Logger.log(getAID().getLocalName() + " : random move");
            }
        }
    }
}
