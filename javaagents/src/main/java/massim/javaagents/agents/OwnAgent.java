/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package massim.javaagents.agents;

import eis.iilang.Action;
import eis.iilang.Identifier;
import eis.iilang.Numeral;
import eis.iilang.Percept;
import massim.javaagents.MailService;
import massim.javaagents.percept.*;

import java.util.*;

import static massim.javaagents.agents.WarpAgent.stringParam;

public class OwnAgent extends Agent {
    private AgentPercepts percept = new AgentPercepts();
    private Queue<Action> toBeDone = new LinkedList<>();

    private String assignedJobStr;
    private job currentJob;
    private Set<String> jobsPercept = new HashSet<>();

    private String shop;
    private shop shopInstance;


    private String lastAction;
    private String lastActionResult;

    private Set<String> jobsTaken = new HashSet<>();
    private boolean isDeliverJob;

    private auction currentMission;
    private auction currentAuction;

    private boolean mustBeCharged;

    public OwnAgent(String name, MailService mailbox) {
        super(name, mailbox);
    }

    private boolean lastJobWasSuccessful(String jobTitle) {
        return (lastAction.equals(jobTitle)) && (lastActionResult.equals("successful"));
    }

    private void charge() {
        if (lastJobWasSuccessful("charge")) {
            if (percept.getSelfInfo().getCharge() > percept.getSelfRole().getBattery() - 10) {
                mustBeCharged = false;
                toBeDone.clear();
            }
            else {
                toBeDone.add(new Action("charge"));
            }
            return;
        }
        int routeLength = percept.getRouteLength();
        if (percept.getRoutes().size() > 1 && routeLength > 1) {
            if (percept.getSelfInfo().getCharge() < 20)
                toBeDone.add(new Action("recharge"));
            else
                toBeDone.add(new Action("goto", new Identifier(findNearestChargeStation())));
        } else {
            toBeDone.add(new Action("goto", new Identifier(findNearestChargeStation())));
            toBeDone.add(new Action("charge"));
        }

    }

    @Override
    public void handlePercept(Percept percept) {
    }

    private boolean checkCharge() {
        return percept.getSelfInfo().getCharge() >= 100;
    }

    private void handleLastActionResult(String res) {
        if (res != null)
            if (res.equals("successful")) {
            } else if (res.equals("failed_capacity")) {
            } else if (res.equals("failed_job_status")) {
                currentJob = null;
                assignedJobStr = null;
            }
    }

    @Override
    public Action step() {
        // todo : first location near --- testing
        // Todo : charge again
        // Todo : dynamic charge again strategy --- bug
        // todo : don't check until receive --- bug
        // todo : loop shop
        // todo : give and receive (token-ring)
        // todo : bikhial when error in deliver or buy, access next job
        // todo : time limit for jobs
        // todo : error handling
        // todo : failed capacity
        // todo : resource nodes (observability)
//      POLL IMMEDIATELY

        if (toBeDone.size() > 0) {
            return toBeDone.poll();
        }

//      GET & SET PERCEPTS

        percept.setPercepts(this.getPercepts());
        if (getStepNumber() != 0) {
            percept.stepPercept();
        } else {
            percept.initialize();
        }
        percept.getSelfInfo().setName(this.getName());

//        SET LAST ACTION INFO

        lastAction = percept.getSelfInfo().getLastAction();
        lastActionResult = percept.getSelfInfo().getLastActionResult();
        handleLastActionResult(lastActionResult);

//

        if (assignedJobStr == null) {
            jobsPercept = new HashSet<>(percept.Jobs.keySet());
            jobsPercept.removeAll(jobsTaken);
            if (jobsPercept.size() > 0) {
                assignedJobStr = jobsPercept.iterator().next();
                currentJob = percept.Jobs.get(assignedJobStr);
                jobsTaken.add(assignedJobStr);
                broadcast(new Percept("taken", new Identifier(assignedJobStr)), getName());
            }
        }
        if (assignedJobStr != null) {
            if ((lastAction.compareTo("buy") == 0) && (lastActionResult.compareTo("successful") == 0)) {
                isDeliverJob = true;
                toBeDone.clear();
            }
            if (lastAction.compareTo("deliver_job") == 0 && lastActionResult.compareTo("successful") == 0) {
                isDeliverJob = false;
                assignedJobStr = null;
                toBeDone.clear();
            }
            
            if (checkCharge()) {
                int performJobStatus = performJob();
                if (performJobStatus < 0){
                    currentJob = null;
                    assignedJobStr = null;
                }
            } else {
                charge();
                return new Action("recharge");
            }
            
            if (currentJob == null) {
                return new Action("recharge");
            }
            
        }
        if (toBeDone.size() > 0) {
            return toBeDone.poll();
        } else {
            return new Action("recharge");
        }
    }

    @Override
    public void handleMessage(Percept message, String sender) {
        switch (message.getName()) {
            case "taken":
                jobsTaken.add(stringParam(message.getParameters(), 0));
                break;
        }
    }

    private boolean inTheShop(shop shopInstance) {
        double myLat = percept.getSelfInfo().getLat();
        double myLon = percept.getSelfInfo().getLon();

        double shopLat = shopInstance.getShopLat();
        double shopLon = shopInstance.getShopLon();

        return (myLat == shopLat && myLon == shopLon);
    }

    private int performJob() {
        /*if (currentJob.getJobEnd() > this.getStepNumber() + percept.getRouteLength()){
            return -1;
        }*/
        if (isDeliverJob) {
            toBeDone.add(new Action("goto", new Identifier(currentJob.getJobStorage())));
            toBeDone.add(new Action("deliver_job", new Identifier(assignedJobStr)));
        } else {

            for (int i = 0; i < currentJob.getJobRequireds().size(); i++) {

                String itemName = currentJob.getJobRequireds().get(i).getLeft();
                int amount = currentJob.getJobRequireds().get(i).getRight();

                shopInstance = null;
                List<shop> shops = percept.shopsByItem.get(itemName);

                if (shop == null && shops != null && shopInstance != null) {
                    shopInstance = findNearestshop(currentJob.getJobStorage(), itemName, amount);
                    if (shopInstance == null){
                        return -1;
                    }
                    shop = shopInstance.getShopName();
                }
//                TODO: Fix
//                if (inTheShop(shopInstance)) {
                if (lastActionResult.compareTo("successful") == 0) {
                    toBeDone.add(new Action("buy", new Identifier(itemName), new Numeral(amount)));
                    toBeDone.add(new Action("goto", new Identifier(shop)));
                } else {
                    toBeDone.add(new Action("goto", new Identifier(shop)));
                }
            
            }
        }
        return 1;
    }

    private shop findNearestshop(String storage, String itemName, int itemAmount) {
        double minDistance = Double.MAX_VALUE;
        shop shopInstance = null;
        storage tempStorage = percept.Storages.get(storage);
        List<shop> shops = percept.shopsByItem.get(itemName);
        if (shops != null) {
            for (shop sh : shops){
                double shopLat = sh.getShopLat(), shopLon = sh.getShopLon(), storageLat = tempStorage.getLat(), storageLon = tempStorage.getLon();
                double dist = findLinearDistance(shopLat, shopLon, storageLat, storageLon);
                if (dist < minDistance) {
                    if (sh.ShopItemsMap.get(itemName).getAmount() >= itemAmount) {
                        minDistance = dist;
                        shopInstance = sh;
                    }
                }
            }
        } else {
            currentJob = null;
            assignedJobStr = null;
            return null;
        }
        return shopInstance;
    }

    private double findLinearDistance(double d1Lat, double d1Lon, double d2Lat, double d2Lon) {
        return Math.sqrt(Math.pow((d1Lat - d2Lat), 2) + Math.pow((d1Lon - d2Lon), 2));
    }

    private String findNearestChargeStation() {
        double minDistance = Double.MAX_VALUE;
        String chargeStation = "";
        for (chargingStation cs : percept.getChargingStations()) {
            double stationLat = cs.getLat(), stationLon = cs.getLon(), myLat = percept.getSelfInfo().getLat(), myLon = percept.getSelfInfo().getLon();
            double dist = findLinearDistance(stationLat, stationLon, myLat, myLon);
            if (dist < minDistance) {
                minDistance = dist;
                chargeStation = cs.getName();
            }
        }
        return chargeStation;
    }
}
