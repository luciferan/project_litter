import java.io.*;
import java.net.*;
import java.util.*;

public class svr_main
{
	public static svr_dataManager dataManager = new svr_dataManager();

	//
	public static void main(String args[])
	{
		System.out.println("svr_main: start.");
		
		//
		Thread workerThread = null;
		//Thread senderThread = null;

		Scanner sc = null;
		String cmd;

		//
		try
		{
			workerThread = new Thread(new svr_workerThread(dataManager));
			//senderThread = new Thread(new svr_senderThread(dataManager));
			workerThread.start();
			//senderThread.start();

			//
			sc = new Scanner(System.in);
			while( true )
			{
				//System.out.print("cmd>");
				cmd = sc.next();
				
				if( cmd.equals("exit") )
					break;
			}
			
			//
			System.out.println("svr_main: end.");
		}
		finally
		{
			if( null != sc )
				sc.close();
			
			workerThread.interrupt();
			//senderThread.interrupt();
		}
		
		System.out.println("svr_main: finally");
	}
}