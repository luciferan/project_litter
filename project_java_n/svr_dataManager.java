import java.io.*;
import java.net.*;
import java.util.*;

class svr_dataManager
{
	svr_dataManager() {}
	
	//
	// session unique index maker
	static uniqueIndexMaker uniqueIndexer = new uniqueIndexMaker();
	long makeSessionKey() { return uniqueIndexer.getUniqueIndex(); }
		
	// mapSessionList: HashMap<Long, svr_session>
	HashMap<Long, svr_session> mapSessionList = new HashMap<Long, svr_session>();
	HashMap<Long, svr_session> getSessionList(){ return mapSessionList; }

	synchronized boolean addSession(long index, svr_session session)
	{
		if( true == mapSessionList.containsKey(index) )
			return false;
		mapSessionList.put(index, session);
		return true;
	}
	
	synchronized boolean delSession(long index)
	{
		if( false == mapSessionList.containsKey(index) )
			return false;

		mapSessionList.remove(index);
		return true;
	}
	
	svr_session getSession(long index)
	{
		svr_session s = null;
		s = mapSessionList.get(index);
		if( null == s )
			return null;
		
		return s;
	}
	
	//
	HashSet<Thread> sessionProcThreadList = new HashSet<Thread>();
	HashSet<Thread> getSessionThread() { return sessionProcThreadList; }

	synchronized boolean addSessionThread(Thread t)
	{
		if( true == sessionProcThreadList.contains(t) )
			return false;
		sessionProcThreadList.add(t);
		return true;
	}
	synchronized boolean delSessionThread(Thread t)
	{
		if( false == sessionProcThreadList.contains(t) )
			return false;
		sessionProcThreadList.remove(t);
		return true;
	}

	//
}