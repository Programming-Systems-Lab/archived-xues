package psl.xues.ed;

import java.util.*;
import siena.*;

/**
 * Tester class for EDBus.
 * NOTE: Changing Comparable of Subscriber on the fly is not tested.
 *
 * Created: Wed Jun 13 16:57:03 2001
 *
 * @author James Wu
 */

public class EDBusTester {
    public static void main(String[] args){
	
	absorptionTest0(false);
	absorptionTest1(false);
	absorptionTest2(false);
	reorderingTest(false);
	unsubscribeTest0(false);
    }


    /* No absorption at all */
    public static void absorptionTest0(boolean verbose){
	EDBus bus = new EDBus();
	final StringBuffer buff = new StringBuffer();



	EDNotifiable consumer0 = new EDNotifiable(){
		public boolean notify(Notification e){
		    buff.append("2nd\n");
		    return false;
		}
		
	    };
	Filter f0 = new Filter();
	f0.addConstraint("timestamp",
			 new AttributeConstraint(Op.ANY,(AttributeValue)null));
	bus.subscribe(f0,consumer0,new Integer(2));



	EDNotifiable consumer1 = new EDNotifiable(){
		public boolean notify(Notification e){
		    buff.append("1st\n");
		    return false;
		}
		
	    };
	Filter f1 = new Filter();
	f1.addConstraint("timestamp",
			 new AttributeConstraint(Op.ANY,(AttributeValue)null));
	bus.subscribe(f1,consumer1,new Integer(1));




	EDNotifiable consumer2 = new EDNotifiable(){
		public boolean notify(Notification e){
		    buff.append("0th\n");
		    return false;
		}
		
	    };
	Filter f2 = new Filter();
	f2.addConstraint("timestamp",
			 new AttributeConstraint(Op.ANY,(AttributeValue)null));
	bus.subscribe(f2,consumer2,new Integer(0));



	Notification e = new Notification();
	e.putAttribute("Name","Notification1");
	e.putAttribute("timestamp",System.currentTimeMillis());
	bus.publish(e);
	bus.shutdown();

	if(verbose){
	    System.out.println(buff);
	}
	if(buff.toString().equals("0th\n1st\n2nd\n")){
	    System.out.println("Absorption Test 0 Passed");
	}else{
	    System.out.println("Absorption Test 0 Failed");
	}

    }
    
    /* Absorption after the first EDNotifiable */
    public static void absorptionTest1(boolean verbose){
	EDBus bus = new EDBus();
	final StringBuffer buff = new StringBuffer();



	EDNotifiable consumer0 = new EDNotifiable(){
		public boolean notify(Notification e){
		    buff.append("2nd\n");
		    return false;
		}
		
	    };
	Filter f0 = new Filter();
	f0.addConstraint("timestamp",
			 new AttributeConstraint(Op.ANY,(AttributeValue)null));
	bus.subscribe(f0,consumer0,new Integer(2));



	EDNotifiable consumer1 = new EDNotifiable(){
		public boolean notify(Notification e){
		    buff.append("1st\n");
		    return false;
		}
		
	    };
	Filter f1 = new Filter();
	f1.addConstraint("timestamp",
			 new AttributeConstraint(Op.ANY,(AttributeValue)null));
	bus.subscribe(f1,consumer1,new Integer(1));




	EDNotifiable consumer2 = new EDNotifiable(){
		public boolean notify(Notification e){
		    buff.append("0th\n");
		    return true;
		}
		
	    };
	Filter f2 = new Filter();
	f2.addConstraint("timestamp",
			 new AttributeConstraint(Op.ANY,(AttributeValue)null));
	bus.subscribe(f2,consumer2,new Integer(0));



	Notification e = new Notification();
	e.putAttribute("Name","Notification1");
	e.putAttribute("timestamp",System.currentTimeMillis());
	bus.publish(e);
	bus.shutdown();

	if(verbose){
	    System.out.println(buff);
	}
	if(buff.toString().equals("0th\n")){
	    System.out.println("Absorption Test 1 Passed");
	}else{
	    System.out.println("Absorption Test 1 Failed");
	}

    }
    
    /* Absorption after the last EDNotiable(No absorption at all)*/
    public static void absorptionTest2(boolean verbose){
	EDBus bus = new EDBus();
	final StringBuffer buff = new StringBuffer();



	EDNotifiable consumer0 = new EDNotifiable(){
		public boolean notify(Notification e){
		    buff.append("2nd\n");
		    return true;
		}
		
	    };
	Filter f0 = new Filter();
	f0.addConstraint("timestamp",
			 new AttributeConstraint(Op.ANY,(AttributeValue)null));
	bus.subscribe(f0,consumer0,new Integer(2));



	EDNotifiable consumer1 = new EDNotifiable(){
		public boolean notify(Notification e){
		    buff.append("1st\n");
		    return false;
		}
		
	    };
	Filter f1 = new Filter();
	f1.addConstraint("timestamp",
			 new AttributeConstraint(Op.ANY,(AttributeValue)null));
	bus.subscribe(f1,consumer1,new Integer(1));




	EDNotifiable consumer2 = new EDNotifiable(){
		public boolean notify(Notification e){
		    buff.append("0th\n");
		    return false;
		}
		
	    };
	Filter f2 = new Filter();
	f2.addConstraint("timestamp",
			 new AttributeConstraint(Op.ANY,(AttributeValue)null));
	bus.subscribe(f2,consumer2,new Integer(0));



	Notification e = new Notification();
	e.putAttribute("Name","Notification1");
	e.putAttribute("timestamp",System.currentTimeMillis());
	bus.publish(e);
	bus.shutdown();

	if(verbose){
	    System.out.println(buff);
	}
	if(buff.toString().equals("0th\n1st\n2nd\n")){
	    System.out.println("Absorption Test 2 Passed");
	}else{
	    System.out.println("Absorption Test 2 Failed");
	}

    }


    /***************************************************************/
    /***************************************************************/


    public static void reorderingTest(boolean verbose){
	EDBus bus = new EDBus();
	final StringBuffer buff = new StringBuffer();
	
	EDNotifiable consumer0 = new EDNotifiable(){
		
		public boolean notify(Notification e){
		    buff.append("Consumer0,"+e.getAttribute("Name")+"\n");
		    return false;
		}

	    };

	Filter f0 = new Filter();
	f0.addConstraint("timestamp",
			new AttributeConstraint(Op.ANY,(AttributeValue)null));

	bus.subscribe(f0,consumer0,new Integer(1));
	EDNotifiable consumer1 = new EDNotifiable(){
		
		public boolean notify(Notification e){
		    buff.append("Consumer1,"+e.getAttribute("Name")+"\n");
		    return false;
		}

	    };

	Filter f1 = new Filter();
	f1.addConstraint("timestamp",
			 new AttributeConstraint(Op.ANY,(AttributeValue)null));
	f1.addConstraint("Name",
			 new AttributeConstraint(Op.ANY,(AttributeValue)null));




	bus.subscribe(f1,consumer1, new Integer(0));

	if(verbose){
	    System.out.println("\n");
	    System.out.println("Between notification 0 and 1,"+
			       "Out of order time is less than threshold,"+
			       "reordering should be successful.");
	    System.out.println("Between notification 2 and 3,"+
			       "Out of order time is more than threshold,"+
			       "reordering should fail.\n");
	}

	Notification e = new Notification();
	e.putAttribute("Name","Notification1");
	e.putAttribute("timestamp",System.currentTimeMillis());
	bus.publish(e);
	try{
	    Thread.currentThread().sleep(700);
	}catch(Exception exc){}

	e = new Notification();
	e.putAttribute("Name","Notification0");
	e.putAttribute("timestamp",System.currentTimeMillis()-1200);
	bus.publish(e);




	e = new Notification();
	e.putAttribute("Name","Notification3");
	e.putAttribute("timestamp",System.currentTimeMillis());
	bus.publish(e);
	try{
	    Thread.currentThread().sleep(1200);
	}catch(Exception exc){}

	e = new Notification();
	e.putAttribute("Name","Notification2");
	e.putAttribute("timestamp",System.currentTimeMillis()-1300);
	bus.publish(e);
	bus.shutdown();
	
	if(verbose){
	    System.out.println(buff);
	}	

	if(buff.toString().equals("Consumer1,\"Notification0\"\n"
				  +"Consumer0,\"Notification0\"\n"
				  +"Consumer1,\"Notification1\"\n"
				  +"Consumer0,\"Notification1\"\n"
				  +"Consumer1,\"Notification3\"\n"
				  +"Consumer0,\"Notification3\"\n"
				  +"Consumer1,\"Notification2\"\n"
				  +"Consumer0,\"Notification2\"\n")){
	    System.out.println("Reordering Test Passed");
	}else{
	    System.out.println("Reordering Test Failed");
	}
    }

    public static void unsubscribeTest0(boolean verbose){
	EDBus bus = new EDBus();
	final StringBuffer buff = new StringBuffer();

	EDNotifiable consumer0 = new EDNotifiable(){
		
		public boolean notify(Notification e){
		    buff.append("Consumer0,"+e.getAttribute("Name")+"\n");
		    return false;
		}

	    };

	Filter f = new Filter();
	f.addConstraint(null,
			new AttributeConstraint(Op.ANY,(AttributeValue)null));

	bus.subscribe(f,consumer0,new Integer(1));


	EDNotifiable consumer1 = new EDNotifiable(){
		
		public boolean notify(Notification e){
		    buff.append("Consumer1,"+e.getAttribute("Name")+"\n");
		    return false;
		}

	    };

	bus.subscribe(f,consumer1,new Integer(3));

	EDNotifiable consumer2 = new EDNotifiable(){
		
		public boolean notify(Notification e){
		    buff.append("Consumer2,"+e.getAttribute("Name")+"\n");
		    return false;
		}

	    };

	bus.subscribe(f,consumer2,new Integer(5));
	
	Notification e = new Notification();
	e.putAttribute("Name","Notification0");
	//e.putAttribute("timestamp",System.currentTimeMillis());
	bus.publish(e);
	
	bus.flush();
	bus.unsubscribe(consumer1);
	e = new Notification();
	e.putAttribute("Name","Notification1");
	//e.putAttribute("timestamp",System.currentTimeMillis());
	bus.publish(e);

	bus.shutdown();
	if(verbose){
	    System.out.println(buff);
	}
	if(buff.toString().equals("Consumer0,\"Notification0\"\n"
				  +"Consumer1,\"Notification0\"\n"
				  +"Consumer2,\"Notification0\"\n"
				  +"Consumer0,\"Notification1\"\n"
				  +"Consumer2,\"Notification1\"\n")){
	    System.out.println("Unsubscription Test 0 Passed");
	}else{
	    System.out.println("Unsubscription Test 0 Failed");
	}

    }



}// EDBusTester






