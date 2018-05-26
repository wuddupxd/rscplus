package Game;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import Client.Logger;

public class ReplayServer implements Runnable {
	String playbackDirectory;
	DataInputStream input = null;
	
	ReplayServer(String directory) {
		playbackDirectory = directory;
	}
	
	@Override
	public void run() {
		ServerSocket sock = null;
		try {
			input = new DataInputStream(new FileInputStream(new File(playbackDirectory + "/in.bin")));
			
			Logger.Info("ReplayServer: Waiting for client...");
			
			sock = new ServerSocket(43594);
			Socket client = sock.accept();
			
			Logger.Info("ReplayServer: Starting playback");
			
			long timestamp_input = input.readLong();
			long timestamp_adjust = System.currentTimeMillis();
			for (;;) {
				long time = System.currentTimeMillis() - timestamp_adjust;
				if (time >= timestamp_input) {
					int length = input.readInt();
					byte[] data = new byte[length];
					input.read(data, 0, length);
					
					client.getOutputStream().write(data, 0, length);
					timestamp_input = input.readLong();
				}
			}
		} catch (Exception e) {
			if (sock != null) {
				try { sock.close(); } catch (Exception e2) {}
			}
			
			Logger.Error("ReplayServer: Failed to serve replay");
		}
	}
}
