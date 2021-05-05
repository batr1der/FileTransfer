package main;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Arrays;

public class FileTransfer {

	Socket socket;
	BufferedInputStream netins;
	BufferedOutputStream netouts;
	DataInputStream dis;
	DataOutputStream dos;
	
	FileInputStream fileins;
	FileOutputStream fileouts;
	
	String relativePath = "";
	
	private void initStreams() throws IOException {
		netins = new BufferedInputStream(socket.getInputStream());
		netouts = new BufferedOutputStream(socket.getOutputStream());
		dis = new DataInputStream(netins);
		dos = new DataOutputStream(netouts);
	}
	
	public FileTransfer(Socket socket) throws IOException {
		this.socket = socket;
		initStreams();
	}
	
	private String getDirectoryType(File file) {
		if(file.isFile()) {
			return "file";
		} else {
			return "folder";
		}
	}
	
	public void send(String filepath) throws IOException {
		File file = new File(filepath);
		
		
		if(file.isFile()) {
			sendFile(file);
		} else {
			sendFolder(file);
		}
	}
	
	public void get() throws IOException {
		String metaDirectoryName = dis.readUTF();
		String metaDirectoryType = dis.readUTF();
		
		String directoryName = metaDirectoryName.split(": ")[1];
		String directoryType = metaDirectoryType.split(": ")[1];
		
		if(directoryType.equals("file")) {
			String metaContentLength = dis.readUTF();
			int contentLength = Integer.valueOf(metaContentLength.split(": ")[1]);

			getFile(directoryName, contentLength);
		} else {
			String metaAmountFiles = dis.readUTF();
		
			int amountFiles = Integer.valueOf(metaAmountFiles.split(": ")[1]);
			
			getFolder(directoryName, amountFiles);
		}
	}
	
	public void sendFolder(File file) throws IOException {

		String[] meta = {"DirectoryName: " + file.getName(),
				      "DirectoryType: " + getDirectoryType(file),
				      "AmountOfFiles: " + String.valueOf(file.listFiles().length)};		      
		
		for(int i=0; i<meta.length; i++) {
			dos.writeUTF(meta[i]);
		}
		dos.flush();
		
		
		File[] dirs = file.listFiles();
		
		for(File dir : dirs) {
			if(dir.isFile()) {
				sendFile(dir);
			} else {
				sendFolder(dir);
			}
		}
	}

	public void getFolder(String directoryName, int amountFiles) throws IOException {
		relativePath += directoryName + "/";
		File folder = new File(relativePath);
		folder.mkdir();
	
		for(int i=0; i < amountFiles; i++) {
			get();
		}
		
		relativePath = relativePath.substring(0, relativePath.lastIndexOf(folder.getName()+"/"));
	}
	
	public void sendFile(File file) throws IOException {
		String[] meta = {"DirectoryName: " + file.getName(), 
					  "DirectoryType: " + getDirectoryType(file),
					  "ContentLength: " + file.length()};

		for(int i=0; i<meta.length; i++) {
			dos.writeUTF(meta[i]);
		}
		dos.flush();
		
		FileInputStream fileins = new FileInputStream(file);
		
		byte[] buffer = new byte[1024];
		int length;
		
		for(int i=0; i<Math.ceil((float) file.length()/1024); i++) {
			length = fileins.read(buffer);
			dos.write(buffer, 0, length);
		}
		
		dos.flush();
	}
	
	public void getFile(String directoryName, int contentLength) throws IOException {
		fileouts = new FileOutputStream(new File(relativePath+directoryName));
			
		byte[] buffer = new byte[1024];
		int length = 1024;
		
		for(int i=0; i<Math.ceil((float) contentLength/1024); i++) {
			
			if(i+1 == Math.ceil((float) contentLength/1024)) {
				length = contentLength % 1024;
				
				if(length == 0) {
					dis.readFully(buffer, 0, 1024);
					fileouts.write(buffer, 0, 1024);
					
					break;
				} 
			}
	
			dis.readFully(buffer, 0, length);
			fileouts.write(buffer, 0, length);
			
		}
	
		fileouts.flush();
		fileouts.close();
		
	}
}
