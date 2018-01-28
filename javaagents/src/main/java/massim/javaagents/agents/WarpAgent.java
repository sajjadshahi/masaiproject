package massim.javaagents.agents;

import massim.javaagents.percept.AgentPercepts;
import eis.EnvironmentInterfaceStandard;
import eis.iilang.*;
import massim.javaagents.MailService;

import java.util.*;
import static massim.javaagents.agents.WarpAgent.stringParam;

/**
 * This agent is intended to be used with the QuickTest.json config.
 * It assumes it can warp to any place with the goto action (since it moves incredibly fast).
 * Also, it plans very statically and may break easily (because of that).
 * Also, it cannot assemble yet.
 */
public class WarpAgent extends Agent{

    private Set<String> jobsTaken = new HashSet<>();
    private String myJob;

    private Queue<Action> actionQueue = new LinkedList<>();

    private boolean test = false;

    private Percept shop;
    
    ///*** New feature that we needed
    private boolean flag;
    private AgentPercepts AP = new AgentPercepts(); 
    ///***
    
    /**
     * Constructor.
     *
     * @param name    the agent's name
     * @param mailbox the mail facility
     */
    public WarpAgent(String name, MailService mailbox) {
        super(name, mailbox);
        flag = false;
        //System.out.println("WWWWWWWWWWWWW");
        
    }
    
    @Override
    public void handlePercept(Percept percept) {} // this is not configured to be called

    @Override
    public Action step() {
        //
        
        List<Percept> percepts = new Vector<>();
        percepts = this.getPercepts();
        
        ///*** Changing percepts handle
        AP.setPercepts(percepts);
        if(getStepNumber() != 0) //Step Percept
        {
            AP.stepPercept();
        }
        else //Initial Percept
        {
            AP.initialize();
        }
        ///***
        
        Map<String, Percept> InitialItems = new HashMap<>();
         for (Percept p: percepts){  
            switch(p.getName()){
                // parse info that is always needed
                case "item":
                   // System.out.println("###################"+p.getParameters().toString());
                    
                    InitialItems.putIfAbsent(stringParam(p.getParameters(), 0), p);
                    Set<String> Itemsset = new HashSet<>(InitialItems.keySet());
                    Iterator it = Itemsset.iterator();
                    while(it.hasNext())
                    {
                        Percept Item123 = InitialItems.get(it.next());
                        eis.iilang.Function f= (eis.iilang.Function) Item123.getParameters().get(3);
                        //System.out.println("----------------\nname="+f.getName() + "\nSize = "+f.getClonedParameters().size()+"\n---------------------------");
                        //System.out.println("first child class is: "+f.getParameters().get(0).getClass());
                        eis.iilang.ParameterList ppp = (eis.iilang.ParameterList) f.getParameters().get(0);
                        if (ppp.size() != 0) ///If this item has subitems 
                            ;//System.out.println("+ This item has subitems! ");
                        else
                            ;//System.out.println("- This item does not have subitems! ");
                    }
                    break;
                case "role":
                    // System.out.println("!!!!!!!!!!!!!!" + p.getParameters().toString());
                }
         }
        //

        Map<String, Percept> currentJobs = new HashMap<>();
        Map<String, List<Percept>> shopsByItem = new HashMap<>();        
        String lastActionResult = "";
        String lastAction = "";
        String carriedItems = "";
        Vector<String> resourceNodes = new Vector<>();
        String lon = "";
        String lat = "";        

        for (Percept p: getPercepts()){
            //System.out.println("percept : " + p.toProlog());
            switch(p.getName()){
                // parse info that is always needed
                case "lastAction":  
                    lastAction = stringParam(p.getParameters(), 0);
                    //System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! LastAction :["+lastAction+"]");
                    break;
                case "lastActionParams":
                    break;
                case "lastActionResult":
                    lastActionResult = stringParam(p.getParameters(), 0);
                   // System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! LastActionResult :["+stringParam(p.getParameters(), 0)+"]");
                    break;
                case "items":
                    //System.out.println("<<<<<<<<<<<<<<<<<<<<<<<<<<<<" + p.toProlog());
                    carriedItems += " " + p.toProlog();
                    break;
                case "resourceNode":
                    resourceNodes.add(p.toProlog());
                    break;
                case "lon":
                    lon = p.toProlog();
                    break;
                case "lat":
                    lat = p.toProlog();
                    break;
                case "action":
                    //System.out.println("************** Action : "+p.toProlog());
                    break;
                case "type":
                    //System.out.println("************* Type : "+p.toProlog());
                    break;
            }
            
            if(actionQueue.size() == 0){
                // parse info needed for planning
                switch(p.getName()){
                    case "job":
                        // remember all active jobs
                        currentJobs.putIfAbsent(stringParam(p.getParameters(), 0), p);
                        break;
                    case "shop":
                        // remember shops by what they offer
                        ParameterList stockedItems = listParam(p, 4);
                        for(Parameter stock: stockedItems){
                            if(stock instanceof Function){
                                String itemName = stringParam(((Function) stock).getParameters(), 0);
                                int amount = intParam(((Function) stock).getParameters(), 2);
                                if(amount > 0){
                                    shopsByItem.putIfAbsent(itemName, new ArrayList<>());
                                    shopsByItem.get(itemName).add(p);
                                }
                            }
                        }
                        break;
                }
            }
        }
         
           
        
        //say("Last step I did " + lastAction);

        if(carriedItems.isEmpty()==false){
            //say("I carry " + carriedItems);
        }

        //say("I am at " + lon + " " + lat);

        if(resourceNodes.isEmpty()==false){
            for(String nodeInfo: resourceNodes){
                //say(nodeInfo);
                ;
            }
        }
        //test gather action
        /*if(test==false){
            actionQueue.add(new Action("goto", new Identifier("resourceNode2")));
            actionQueue.add(new Action("gather"));
            test=true;
        }*/

        // follow the plan if there is one
        
        if(actionQueue.size() > 0) 
        {
            return actionQueue.poll();
        }

        if (myJob == null){
            Set<String> availableJobs = new HashSet<>(currentJobs.keySet());
            availableJobs.removeAll(jobsTaken);
            
            //
            Iterator it = availableJobs.iterator();
            while(it.hasNext())
            {
                //System.out.println("(((((((((((((((((((((((((((((((*************)))))))))))))))))))))))))))))))");
                Percept job123 = currentJobs.get(it.next());
                String storage = stringParam(job123.getParameters(), 1);
                ParameterList requirements = listParam(job123, 5);
                for (Parameter requirement : requirements) 
                {
                   // System.out.println("<<<<<<<<<<<<"+job123+">>>>>>>>>>>");
                    if(requirement instanceof Function){
                        String itemName = stringParam(((Function) requirement).getParameters(), 0);
                        int amount = intParam(((Function) requirement).getParameters(), 1);
                        //System.out.println("OOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOO"+itemName + "," + amount);
                        // item ----> ! sub Item
                        // select this job
                    }
                }
               // System.out.println("size : " + requirements.size());
                if (requirements.size() != 1)
                {
                   // jobsTaken.add(myJob);  // remove job with requirements
                }
            }
            Iterator iit = availableJobs.iterator();
            /*while(iit.hasNext())
            {
                Percept job123 = currentJobs.get(iit.next());
                System.out.println("<<<<<<<<<<<<"+job123+">>>>>>>>>>>");
            }*/
            
            //
            
            if(availableJobs.size() > 0){
                myJob = availableJobs.iterator().next(); // set job to agent
                //say("I will complete " + myJob);
                jobsTaken.add(myJob);
                broadcast(new Percept("taken", new Identifier(myJob)), getName());
            }
        }
        if(myJob != null){
            // plan the job
            
            if( (lastAction.compareTo("buy") == 0) &&  (lastActionResult.compareTo("successful") == 0) )
            {
                //System.out.println("If Buy was successful ********************************************************************************");
                flag = true;
                actionQueue.clear();
            }
            if(lastAction.compareTo("deliver_job")==0 && lastActionResult.compareTo("successful") == 0)
            {
                //System.out.println("If delivered job was successful ********************************************************************************");
                flag = false;
            }
         
            // 1. acquire items
            //System.out.println("1. acquire items");
            Percept job = currentJobs.get(myJob);
            if(job == null){
                //say("I lost my job :(");
                myJob = null;
                return new Action("skip");
            }
            String storage = stringParam(job.getParameters(), 1);
            ParameterList requirements = listParam(job, 5);
            if(flag == false)
            {
            for (Parameter requirement : requirements) {
                if(requirement instanceof Function){
                    // 1.1 get enough items of that type
                    //System.out.println("1.1 get enough items of that type");
                    String itemName = stringParam(((Function) requirement).getParameters(), 0);
                    int amount = intParam(((Function) requirement).getParameters(), 1);
                    if(itemName.equals("") || amount == -1){
                        //say("Something is wrong with this item: " + itemName + " " + amount);
                        
                        continue;
                    }
                    // find a shop selling the item
                    //System.out.println("find a shop selling the item");
                    List<Percept> shops = shopsByItem.get(itemName);
                    if(shops != null)
                    {
                    if(shops.size() == 0){
                        //say("I cannot buy the item " + itemName + "; this plan won't work very well.");
                    }
                    
                    else{
                        //say("I will go to the shop first.");
                        // go to the shop
                        //System.out.println("go to the shop");
                        //Percept shop = shops.get(0);
                        if (shop == null)
                            shop = shops.get(0);
                        else
                        {
                            ;//if agenet has recieved to shop buy item and shop = null
                        }
                        ///*****************
                        /* for (Percept sh: shops){  
                             eis.iilang.Numeral latt = (eis.iilang.Numeral) sh.getParameters().toArray()[1];
                             eis.iilang.Numeral lonn = (eis.iilang.Numeral) sh.getParameters().toArray()[2];
                             double shopLat = latt.getValue().doubleValue();
                             double shopLon = lonn.getValue().doubleValue();
                         }*/
                        ///*****************
                        actionQueue.add(new Action("goto", new Identifier(stringParam(shop.getParameters(), 0))));
                        // buy the items
                        //System.out.println("buy the items");
                        actionQueue.add(new Action("buy", new Identifier(itemName), new Numeral(amount)));
                    }
                    }
                }
            }
            }
            
            if (flag == true)
            {
            // 2. get items to storage
            //System.out.println("2. get items to storage");
            actionQueue.add(new Action("goto", new Identifier(storage)));
            // 2.1 deliver items
            //System.out.println("2.1 deliver items");
            actionQueue.add(new Action("deliver_job", new Identifier(myJob)));
            }
            actionQueue.add(new Action("goto", new Identifier(storage)));
        }

        return actionQueue.peek() != null? actionQueue.poll() : new Action("skip");
    }

    @Override
    public void handleMessage(Percept message, String sender) {
        switch (message.getName()){
            case "taken":
                jobsTaken.add(stringParam(message.getParameters(), 0));
                break;
        }
    }

    /**
     * Tries to extract a parameter from a list of parameters.
     * @param params the parameter list
     * @param index the index of the parameter
     * @return the string value of that parameter or an empty string if there is no parameter or it is not an identifier
     */
    public static String stringParam(List<Parameter> params, int index){
        if(params.size() < index + 1) return "";
        Parameter param = params.get(index);
        if(param instanceof Identifier) return ((Identifier) param).getValue();
        return "";
    }

    /**
     * Tries to extract an int parameter from a list of parameters.
     * @param params the parameter list
     * @param index the index of the parameter
     * @return the int value of that parameter or -1 if there is no parameter or it is not an identifier
     */
    private static int intParam(List<Parameter> params, int index){
        if(params.size() < index + 1) return -1;
        Parameter param = params.get(index);
        if(param instanceof Numeral) return ((Numeral) param).getValue().intValue();
        return -1;
    }

    /**
     * Tries to extract a parameter from a percept.
     * @param p the percept
     * @param index the index of the parameter
     * @return the string value of that parameter or an empty string if there is no parameter or it is not an identifier
     */
    private static ParameterList listParam(Percept p, int index){
        List<Parameter> params = p.getParameters();
        if(params.size() < index + 1) return new ParameterList();
        Parameter param = params.get(index);
        if(param instanceof ParameterList) return (ParameterList) param;
        return new ParameterList();
    }
}
