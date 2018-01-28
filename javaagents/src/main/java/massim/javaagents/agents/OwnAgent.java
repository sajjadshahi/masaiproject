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

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import massim.javaagents.MailService;

import static massim.javaagents.agents.WarpAgent.stringParam;

import massim.javaagents.percept.*;

/**
 * @author Erfan and Sajad
 */
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

    public OwnAgent(String name, MailService mailbox) {
        super(name, mailbox);
        isDeliverJob = false;
        System.out.printf("Design by sajad and erfan");
    }

    private boolean lastJobWasSuccessful(String jobTitle) {
        return (lastAction.equals(jobTitle)) && (lastActionResult.equals("successful"));
    }

    private void charge() {
        if (lastJobWasSuccessful("charge")) {
//            ALREADY IN STATION
            if (enoughCharge()) {
                toBeDone.clear();
            } else {
                toBeDone.add(new Action("charge"));
            }
            return;
        }
//        NOT IN STATION
            toBeDone.add(new Action("goto", new Identifier(findNearestChargeStation())));
            toBeDone.add(new Action("charge"));
    }

    @Override
    public void handlePercept(Percept percept) {
    }

    private boolean checkCharge() {
        return percept.getSelfInfo().getCharge() >= 100;
    }

    private boolean enoughCharge(){
        return percept.getSelfInfo().getCharge() > percept.getSelfRole().getBattery() - 10;
    }

    private Pair<job, shop> getBestJob(Set<String> jobs) {
        Pair<shop, Double> bestJobDecision = null;
        double minDist = Double.MAX_VALUE;
        job bestJob = null;
        shop bestShop = null;
        for (String jobStr : jobs) {
            job jobInstance = percept.Jobs.get(jobStr);
            double currentDist = getBestPathToStorage(jobInstance).getRight();
            if (currentDist < minDist) {
                minDist = currentDist;
                bestJob = jobInstance;
                bestShop = getBestPathToStorage(jobInstance).getLeft();
            }
        }
        return new Pair<job, shop>(bestJob, bestShop);
    }

    private Pair<shop, Double> getBestPathToStorage(job jobInstance) {
        shop nearestShop = null;
        double dist = Double.MAX_VALUE;
        storage jobStorage = percept.Storages.get(jobInstance.getJobStorage());
        double storageLat = jobStorage.getLat(), storageLon = jobStorage.getLon();
        List<shop> shops = percept.shopsByItem.get(jobInstance.getJobRequireds().get(0).getLeft());
        if (shops != null) {
            for (shop sh : shops) {
                double myLat = percept.getSelfInfo().getLat(), myLon = percept.getSelfInfo().getLon(), shopLat = sh.getShopLat(), shopLon = sh.getShopLat();
                double distanceFromAgentToShop = findLinearDistance(myLat, myLon, shopLat, shopLon);
                double distanceFromShopToStorage = findLinearDistance(shopLat, shopLon, storageLat, storageLon);
                double outcomeDistance = distanceFromAgentToShop + distanceFromShopToStorage;
                if (outcomeDistance < dist) {
                    dist = outcomeDistance;
                    nearestShop = sh;
                }
            }
        }
        return new Pair<shop, Double>(nearestShop, dist);

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
        // todo : first location near --- done
        // Todo : charge again -- done
        // Todo : dynamic charge again strategy --- bug
        // todo : don't check until receive --- partial done
        // todo : loop shop -- done
        // todo : give and receive (token-ring)
        // todo : bikhial when error in deliver or buy, access next job
        // todo : time limit for jobs --- mastMali
        // todo : error handling --- partialy
        // todo : failed capacity --- done
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
                Pair<job, shop> bestJobShop = getBestJob(jobsPercept);
                currentJob = bestJobShop.getLeft();
                shopInstance = bestJobShop.getRight();
                shop = shopInstance.getShopName();
                assignedJobStr = currentJob.getJobID();
                jobsTaken.add(assignedJobStr);
                broadcast(new Percept("taken", new Identifier(assignedJobStr)), getName());
            }
        }
        if (assignedJobStr != null) {
            if (lastJobWasSuccessful("buy")) {
                isDeliverJob = true;
                toBeDone.clear();
            } else if (lastJobWasSuccessful("deliver_job")) {
                isDeliverJob = false;
                assignedJobStr = null;
                toBeDone.clear();
            }

            if (checkCharge()) {
                int performJobStatus = performJob();
                if (performJobStatus < 0) {
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

    private int performJob() {

        if (isDeliverJob) {
            if (lastActionResult.equals("failed_location")) {
                toBeDone.add(new Action("goto", new Identifier(currentJob.getJobStorage())));
            } else {
                toBeDone.add(new Action("goto", new Identifier(currentJob.getJobStorage())));
                toBeDone.add(new Action("deliver_job", new Identifier(assignedJobStr)));
            }
        } else {
            String itemName = currentJob.getJobRequireds().get(0).getLeft();
            int amount = currentJob.getJobRequireds().get(0).getRight();
            if (lastActionResult.equals("failed_location")) {
                toBeDone.add(new Action("goto", new Identifier(shop)));
            } else {
                toBeDone.add(new Action("goto", new Identifier(shop)));
                toBeDone.add(new Action("buy", new Identifier(itemName), new Numeral(amount)));
            }

        }
        return 1;
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
