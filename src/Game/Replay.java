/**
 *	rscplus
 *
 *	This file is part of rscplus.
 *
 *	rscplus is free software: you can redistribute it and/or modify
 *	it under the terms of the GNU General Public License as published by
 *	the Free Software Foundation, either version 3 of the License, or
 *	(at your option) any later version.
 *
 *	rscplus is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *	GNU General Public License for more details.
 *
 *	You should have received a copy of the GNU General Public License
 *	along with rscplus.  If not, see <http://www.gnu.org/licenses/>.
 *
 *	Authors: see <https://github.com/OrN/rscplus>
 */

package Game;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import Client.Logger;
import Client.Settings;
import Client.Util;

public class Replay {
	static DataOutputStream output = null;
	static DataOutputStream input = null;
	static DataOutputStream keys = null;
	
	public static void initializeReplayRecording() {
		// No username specified, exit
		if (Client.username_login.length() == 0)
			return;
		
		// String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
		String timeStamp = new SimpleDateFormat("MM-dd-yyyy HH.mm.ss").format(new Date());
		
		String recordingDirectory = Settings.Dir.REPLAY + "/" + Client.username_login;
		Util.makeDirectory(recordingDirectory);
		recordingDirectory = recordingDirectory + "/" + timeStamp;
		Util.makeDirectory(recordingDirectory);
		
		try {
			output = new DataOutputStream(new FileOutputStream(new File(recordingDirectory + "/out.bin")));
			input = new DataOutputStream(new FileOutputStream(new File(recordingDirectory + "/in.bin")));
			keys = new DataOutputStream(new FileOutputStream(new File(recordingDirectory + "/keys.bin")));
			
			Logger.Info("Replay recording started");
		} catch (Exception e) {
			output = null;
			input = null;
			keys = null;
			Logger.Error("Unable to create replay files");
		}
	}
	
	public static void closeReplayRecording() {
		// TODO: We do not call this yet, we need to determine when we call it
		// If the user fails to login, for example, we can't call this in Client.init_login()
		
		try {
			output.close();
			input.close();
			keys.close();
			
			Logger.Info("Replay recording stopped");
		} catch (Exception e) {
			Logger.Error("Unable to close replay files");
		}
	}
	
	public static void dumpRawInputStream(byte[] b, int n, int n2, int n5, int bytesread) {
		if (input == null)
			return;
		
		int off = n2 + n5;
		int len = -n5 + n;
		
		try {
			input.writeLong(System.currentTimeMillis()); // timestamp
			input.writeInt(bytesread); // data length
			input.write(b, off, bytesread); // packet data
		} catch (Exception e) {
		}
	}
	
	public static void dumpRawOutputStream(byte[] b, int off, int len) {
		if (output == null)
			return;
		
		try {
			output.writeLong(System.currentTimeMillis()); // timestamp
			output.writeInt(len); // data length
			output.write(b, off, len); // packet data
		} catch (Exception e) {
		}
	}
	
	public static void dumpXTEAKey(int key) {
		if (keys == null)
			return;
		
		try {
			keys.writeInt(key); // data length
		} catch (Exception e) {
		}
	}
}
