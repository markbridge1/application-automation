/*
 * Center for Knowledge Management Kalmanovitz Library, UCSF
 * 
 * The University of California, San Francisco, CA 94143, 415/476-9000 (c) 2012
 * The Regents of the University of California All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * 
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.markbridge.application.automation;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.sshd.ClientChannel;
import org.apache.sshd.ClientSession;
import org.apache.sshd.SshClient;
import org.apache.sshd.client.channel.ChannelShell;
import org.apache.sshd.common.util.NoCloseInputStream;

/**
 *
 * @author mbridge
 */
public class Ssh {
    
    public long SLEEP = 1000L;
    private static final String UTF8 = "UTF-8";
    
    private static BufferedReader SSH_IN = new BufferedReader(new InputStreamReader(System.in));
    
    private SshClient client;
    private ClientSession session;
    private ChannelShell channel;
    private PipedOutputStream pos;
    private ByteArrayOutputStream stdoutBaos = new ByteArrayOutputStream();
    private ByteArrayOutputStream stderrBaos = new ByteArrayOutputStream();
    
    private String host;
    
    private static final ConcurrentHashMap<String, Ssh> LOOKUP_MAP = new ConcurrentHashMap<String, Ssh>();
    
    private Ssh() {
    }
    
    private Ssh(String host) {
        this.host = host;
    }
    
    public synchronized static Ssh connect(String host) {
        Ssh ssh = null;
        
        if(LOOKUP_MAP.get(host) == null) {
            ssh = new Ssh(host);
            ssh.client = SshClient.setUpDefaultClient();
            ssh.client.start();
        } else {
            ssh = LOOKUP_MAP.get(host);
            ssh.session.close(true);
        }
            
        try {
            ssh.session = ssh.client.connect(host, 22).await().getSession();
            int ret = ClientSession.WAIT_AUTH;
            while ((ret & ClientSession.WAIT_AUTH) != 0) {
                ssh.session.authPassword(ssh.getUsername(), ssh.getPassword());
                ret = ssh.session.waitFor(ClientSession.WAIT_AUTH | ClientSession.CLOSED | ClientSession.AUTHED, 0);
            }
            if ((ret & ClientSession.CLOSED) != 0) {
                System.err.println("error connecting");
                System.exit(-1);
            }

            ssh.channel = (ChannelShell) ssh.session.createChannel(ClientChannel.CHANNEL_SHELL);
            PipedInputStream pis = new PipedInputStream();
            ssh.pos = new PipedOutputStream(pis);
            
            //set streams before opening
            //channel.setIn(new NoCloseInputStream(System.in));
            ssh.channel.setIn(new NoCloseInputStream(pis));
            ssh.channel.setOut(ssh.stdoutBaos);
            ssh.channel.setErr(ssh.stderrBaos);
            
            ssh.channel.open();

        } catch(Exception ex) {
            throw new RuntimeException(ex.getMessage());
        }

        LOOKUP_MAP.put(host, ssh);
        
        return ssh;
    }
    
    public synchronized static void close(String host) {
        Ssh ssh = LOOKUP_MAP.get(host);
        if(ssh != null) {
            ssh.close();
        }
    }
    
    /**
     * 
     * @param command
     * @param responseWaitMillis (0L for no wait)
     * @return
     * @throws IOException 
     */
    public String execute(String command, long responseWaitMillis) throws IOException {
        StringBuffer retVal = new StringBuffer();
        
        pos.write(command.concat("\n").getBytes(UTF8));
        
        if(responseWaitMillis != 0) {
            try {
                Thread.sleep(responseWaitMillis);
            } catch (InterruptedException ex) {
                Logger.getLogger(Ssh.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        if(stdoutBaos.size() > 0) {
            retVal.append( new String(stdoutBaos.toByteArray()) );
        }
        if(stderrBaos.size() > 0) {
            retVal.append( "\n\n(!!!stderr: ".concat(new String(stderrBaos.toByteArray())) );
        }
        
        stdoutBaos.reset();
        stderrBaos.reset();
        
        return retVal.toString();
    }
    
    /**
     * executes command and waits default SLEEP ms before returning
     * @param command
     * @return
     * @throws IOException 
     */
    public String execute(String command) throws IOException {
        return execute(command, SLEEP);
    }
    
    public void close() {
        try {
            channel.close(false);
            channel.waitFor(ClientChannel.CLOSED, 0);
            session.close(true);
        } catch(Exception ex) {
        } finally {
            client.stop();
        }
        LOOKUP_MAP.remove(host);
    }
    
    public String getUsername() {
        
        String username = null;
        
        Console c = System.console();
        if(c != null) {
            c.format("[%s] ", "username");
            username = c.readLine();
        } else {
            try {
                System.out.print("username: ");
                username = SSH_IN.readLine();
            } catch(Exception ex) {
                System.out.println(ex.getMessage());
            }
        }
        
        return username;
    }
    
    public String getPassword() {
        
        String password = null;
        
        Console c = System.console();
        if(c != null) {
            c.format("[%s] ", "password");
            char[] passwdCharArr = c.readPassword();
            password = new String(passwdCharArr);
            Arrays.fill(passwdCharArr, ' ');
            return password;
        } else {
            try {
                System.out.print("password: ");
                password = SSH_IN.readLine();
            } catch(Exception ex) {
                System.out.println(ex.getMessage());
            }
        }
        
        return password;
    }
    
    public String getHost() {
        return host;
    }
    
    public static void main(String[] args)  {
        Ssh ssh = Ssh.connect(args[0]);
        String read = "\n";
        while(! read.equals("exitssh")) {
            try {
                System.out.println(ssh.execute(read));
                read = SSH_IN.readLine();
            } catch(Exception ex) {
                System.out.println(ex.getMessage());
            }
        }
        
        try {
            ssh.execute("exit");
        } catch(Exception ex) {
        }
        
        ssh.close();
    }
}
