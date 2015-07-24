import java.applet.*;
import java.awt.*;
import com.spacecode.sdk.device.*;
import com.spacecode.sdk.device.Device;
import com.spacecode.sdk.device.data.DeviceStatus;
import com.spacecode.sdk.device.data.Inventory;
import com.spacecode.sdk.device.data.PluggedDevice;
import com.spacecode.sdk.device.event.*;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.*;
import java.security.PrivilegedAction;
import java.security.AccessController;
import java.util.List;

import jssc.SerialPortList;
import netscape.javascript.*;

public class RFApp extends Applet implements ActionListener
{

	static JSObject window;
	public static Device localdev;
	public static RFIDEventsHandler eventHndler;

	public int count = 0;
	public String msg = "";
	//public static Map<String, PluggedDeviceInformation> pluggedDevices;
	public static PluggedDevice pluggedDevices;
	public static String	Path,CSV,importPath;
	public static String	oldUID, newUID,tags,remPort;
	public static Boolean IsContinousOn = false;
	public static HashSet<String> setTags;
    public static String retMsg= "";
	//Button scanRfid;
	//java.awt.List tagList;


	public void init()
	{
		setLayout(new FlowLayout());
		//scanRfid = new Button("Scan");
		//tagList = new java.awt.List();
		//add(scanRfid);
		//add(tagList);

		//scanRfid.addActionListener(this);


		setLayout(new FlowLayout());
		window = JSObject.getWindow(this);
		//connectDev();
	}

	@Override
	public void actionPerformed(ActionEvent evt) {
		/*if (evt.getSource() == scanRfid)
		{
			try
			{
				ScanRFID();
				repaint();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}*/
	}

	public class RFIDEventsHandler implements DeviceEventHandler,ScanEventHandler,LedEventHandler,BasicEventHandler   {

		Device localDev;
		public RFIDEventsHandler(Device t) {
			localDev = t;
		}

		@Override
		public void scanStarted() {
			//tagList.clear();
			window.call("notifyEvent", new Object[]{"{\"Message\":\"scanStarted\"}"});
		}

		@Override
		public void scanCompleted() {
			try {
                if(IsContinousOn){ScanRFID(); return;}

				Inventory inv = localdev.getLastInventory();

				String retData = "{";
				retData += (inv.getTagsAll().size() > 0) ? "\"TagAll\":" + getCommaSeparatedList(inv.getTagsAll()) : "\"TagAll\":[]";
				retData += (inv.getTagsAdded().size() > 0) ? ",\"TagAdded\":" + getCommaSeparatedList(inv.getTagsAdded()) : ",\"TagAdded\":[]";
				retData += (inv.getTagsRemoved().size() > 0) ? ",\"TagRemoved\":" + getCommaSeparatedList(inv.getTagsRemoved()) : ",\"TagRemoved\":[]";
				retData += (inv.getTagsPresent().size() > 0) ? ",\"TagPresent\":" + getCommaSeparatedList(inv.getTagsPresent()) : ",\"TagPresent\":[]";
                retData += ",\"Message\":\"scanCompleted\"";
                retData += ",\"DeviceSerial\":\"" + localdev.getSerialNumber() + "\"";
				retData += ",\"DeviceType\":\"" + localdev.getDeviceType().toString() + "\"";
				retData += ",\"HardwareVersion\":\"" + localdev.getHardwareVersion() + "\"";
				retData += ",\"Firmware\":\"" + localdev.getSoftwareVersion() + "\"";
				retData += ",\"ErrorMessage\":\"\"}";

				window.call("notifyEvent", new Object[]{retData});
			}catch (Exception exp)
			{
				exp.printStackTrace();
			}
		}

		@Override
		public void tagAdded(String s) {
            if(IsContinousOn && s != null)
            {
                if(setTags.add(s)){ window.call("countEvent",  new Object[] {s});}
                return;
            }
            window.call("countEvent",  new Object[] {s});
		}

		@Override
		public void scanFailed() {
			window.call("notifyEvent", new Object[]{"{\"ErrorMessage\":\"scanFailed\"}"});
            Dispose();
            ScanRFID();
		}

		@Override
		public void scanCancelledByHost() {
			//window.call("notifyEvent", new Object[]{"{\"Message\":\"scanStopped\"}"});
		}


		@Override
		public void deviceDisconnected() {
			pluggedDevices = null;
			window.call("notifyEvent", new Object[]{"{\"Message\":\"deviceDisconnected\"}"});
		}

		@Override
		public void deviceStatusChanged(DeviceStatus deviceStatus) {
			//window.call("notifyEvent", new Object[] {deviceStatus.toString()});
		}

		@Override
		public void lightingStarted(List<String> list) {
			//IsledOn = true;

		}

		@Override
		public void lightingStopped() {
			
		}
	}

	public void start()	
	{
		try{
		
		}
		catch (Exception e) {
		//
		}
	}
	public void paint(Graphics g)
	{
        g.setFont(new Font("TimesRoman", Font.PLAIN, 8));
        g.drawString("RFID",1, 9);
	}

	private void connectDev() {

		//if(pluggedDevices == null) {
		if(pluggedDevices == null) {
			String[] ports = SerialPortList.getPortNames();
			Map<String, PluggedDevice> pluggedDevices1 = Device.getPluggedDevices();
			//pluggedDevices = pluggedDevices1.entrySet().iterator().next().getValue();;
			int i = 0;
			nextPort:
			for ( ;i< ports.length;i++)  {
				try {
					window.call("weightEvent", new Object[]{":" + ports[i]});
					localdev = new Device(null ,ports[i]);
					//window.call("weightEvent", new Object[]{pluggedDevices.getSerialNumber() + ":" + pluggedDevices.getSerialPort()});
					//localdev = new Device(pluggedDevices.getSerialNumber() ,pluggedDevices.getSerialPort());
				}catch (DeviceCreationException exp)
				{
					exp.printStackTrace();
					continue nextPort;
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
				DeviceStatus st = localdev.getStatus();
				if (st == DeviceStatus.READY) {
					remPort = ports[i];
					break;
				}
			}
		}
		else
		{

			try {
				window.call("weightEvent", new Object[]{":" + remPort});
				localdev = new Device(null ,remPort);
				//window.call("weightEvent", new Object[]{pluggedDevices.getSerialNumber() + ":" + pluggedDevices.getSerialPort()});
				//localdev = new Device(pluggedDevices.getSerialNumber() ,pluggedDevices.getSerialPort());
			} catch (DeviceCreationException e) {
				e.printStackTrace();
			}
		}

		if(localdev == null)
		{
			window.call("notifyEvent", new Object[] {"{\"ErrorMessage\":\"No Device Found\"}"});
		}
		else
		{
			eventHndler = new RFIDEventsHandler(localdev);
			localdev.addListener(eventHndler);
		}
	}

	public String test() {
		return "TestFromApplet";	
	}

	public void Dispose() {
		AccessController.doPrivileged(new PrivilegedAction()
		{
			public Object run()
			{
				localdev.release();
				localdev.removeListener(eventHndler);
				localdev = null;
				java.lang.Runtime.getRuntime().freeMemory();
				//this.destroy();
				return null;
			}
		});
	}
	
	public void ScanRFID() {
		AccessController.doPrivileged(new PrivilegedAction()
		{
			public Object run()
			{
				if(localdev == null)
				{
					connectDev();
				}
				//if(IsledOn ){LightOff();}
				localdev.requestScan();
				return null;
			}
		});	
	}

	public void StopScan() {
		AccessController.doPrivileged(new PrivilegedAction() {
			public Object run() {
				localdev.stopScan();
				return null;
			}
		});
	}

	public void LightOn(String _tags) {
		tags = _tags;
        String[] list = tags.split(",");
        List<String> ltgs2 = Arrays.asList(list);
        localdev.startLightingTagsLed(ltgs2);
/*		AccessController.doPrivileged(new PrivilegedAction() {
			public Object run() {

				return null;
			}
		});*/

	}

	public String LightOff() {
        String _retVal = "";
        try {
            Thread.sleep(500);
            _retVal= (localdev.stopLightingTagsLed()) ? "LEDs Stopped" : "LEDs not Stopped";

        }catch (InterruptedException iex)
        {
            _retVal = iex.getMessage();
            iex.printStackTrace();
            //System.out.print(1);
        }
        return _retVal;

        /*
        AccessController.doPrivileged(new PrivilegedAction() {
            public Object run() {

                return null;
            }
        });
        */

    }

	public void WriteTagUID(String _oldUID, String _newUID) {
		oldUID = _oldUID;
		newUID = _newUID;
		AccessController.doPrivileged(new PrivilegedAction() {
			public Object run() {
			localdev.rewriteUid(oldUID, newUID);
				return null;
			}
		});
	}

	public void WaitModeOn() {
        setTags = new HashSet<String>();
        IsContinousOn  = true;
        ScanRFID();
	}

	public  void WaitModeOff() {
        IsContinousOn  = false;
        StopScan();
        Inventory inv =  new Inventory(); //localdev.getLastInventory();
        //inv.
        List<String> list = new ArrayList<String>(setTags);
        String retData = "{";
        retData += (list.size() > 0) ? "\"TagAll\":" + getCommaSeparatedList(list) : "\"TagAll\":[]";
        retData +=  ",\"TagAdded\":[]";
        retData +=  ",\"TagRemoved\":[]";
        retData +=  ",\"TagPresent\":[]";
        retData += ",\"Message\":\"scanCompleted\"";
        retData += ",\"DeviceSerial\":\"" + localdev.getSerialNumber() + "\"";
        retData += ",\"DeviceType\":\"" + localdev.getDeviceType().toString() + "\"";
        retData += ",\"HardwareVersion\":\"" + localdev.getHardwareVersion() + "\"";
        retData += ",\"Firmware\":\"" + localdev.getSoftwareVersion() + "\"";
        retData += ",\"ErrorMessage\":\"\"}";

        window.call("notifyEvent", new Object[]{retData});
	}

	public  void TagAndWeight() {
	}


	public String exportCSV(String path, final String file) {
		Path = path;
		CSV = file;
        AccessController.doPrivileged(new PrivilegedAction() {
            public Object run() {
                try {
                    PrintWriter writer = new PrintWriter(Path, "UTF-8");
                    writer.print(file);
                    writer.close();
                    retMsg = "FileExported";
                } catch (IOException e) {
                    //window.call("notifyEvent", new Object[]{"{\"ErrorMessage\":\"" + e.getMessage() + "\"}"});
                    e.printStackTrace();
                    retMsg = "ErrorMessage:" + e.getMessage();
                }
                return null;
            }
        });
        return retMsg;
	}

	public static String importFile(String path) {
		importPath = path;
		AccessController.doPrivileged(new PrivilegedAction() {
			public Object run() {

		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(importPath));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return "error in import file";
		}
		try {
				StringBuilder sb = new StringBuilder();
				String line = br.readLine();

				while (line != null) {
					sb.append(line);
					sb.append(System.getProperty("line.separator"));
					line = br.readLine();
				}
			br.close();
				return sb.toString();
			}catch(IOException exp)
		{
			exp.printStackTrace();
			return "error in import file";
		}
				//return null;
			}
		});
		return "error in import file";
	}

	public static String getCommaSeparatedList(List<String> list) {
		Set setItems = new LinkedHashSet(list);
		List<String> ads = new ArrayList<String>(setItems);

		Iterator<String> ite = ads.iterator();
		String ret = "[\"" + ite.next() + "\"";
		while (ite.hasNext()) {
			ret += ",\"" + ite.next() + "\"";
		}
		ret += "]";
		return ret;
	}
}
