/******************************************************************
*
*	CyberUPnP for Java
*
*	Copyright (C) Satoshi Konno 2002
*
*	File: SSDPSearchResponseSocket.java
*
*	Revision;
*
*	11/20/02
*		- first revision.
*	05/28/03
*		- Added post() to send a SSDPSearchRequest.
*	
******************************************************************/

package org.cybergarage.upnp.ssdp;

import java.io.IOException;

import org.cybergarage.upnp.*;

public class SSDPSearchResponseSocket extends HTTPUSocket implements Runnable
{
	////////////////////////////////////////////////
	//	Constructor
	////////////////////////////////////////////////

	public SSDPSearchResponseSocket()
	{
		setControlPoint(null);
	}
	
	public SSDPSearchResponseSocket(String bindAddr, int port) throws IOException
	{
		super(bindAddr, port);
		setControlPoint(null);
	}

	////////////////////////////////////////////////
	//	ControlPoint	
	////////////////////////////////////////////////

	private ControlPoint controlPoint = null;
	
	public void setControlPoint(ControlPoint ctrlp)
	{
		this.controlPoint = ctrlp;
	}

	public ControlPoint getControlPoint()
	{
		return controlPoint;
	}

	////////////////////////////////////////////////
	//	run	
	////////////////////////////////////////////////

	private Thread deviceSearchResponseThread = null;
		
	public void run()
	{
		Thread thisThread = Thread.currentThread();
		
		ControlPoint ctrlPoint = getControlPoint();

		while (deviceSearchResponseThread == thisThread) {
			Thread.yield();
			SSDPPacket packet = receive();
			if (packet == null)
				break;
			if (ctrlPoint != null)
				ctrlPoint.searchResponseReceived(packet); 
		}
	}
	
	public void start()
	{
		deviceSearchResponseThread = new Thread(this);
		deviceSearchResponseThread.setName("device search response");
		deviceSearchResponseThread.setDaemon(true);
		deviceSearchResponseThread.start();
	}
	
	public void stop()
	{
		if (deviceSearchResponseThread!=null)
			deviceSearchResponseThread.interrupt();
		deviceSearchResponseThread = null;
	}

	////////////////////////////////////////////////
	//	post
	////////////////////////////////////////////////

	public boolean post(String addr, int port, SSDPSearchResponse res)
	{
		return post(addr, port, res.getHeader());
	}

	////////////////////////////////////////////////
	//	post
	////////////////////////////////////////////////

	public boolean post(String addr, int port, SSDPSearchRequest req)
	{
		return post(addr, port, req.toString());
	}
}

