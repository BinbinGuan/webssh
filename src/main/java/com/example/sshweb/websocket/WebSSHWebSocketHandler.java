package com.example.sshweb.websocket;

import com.example.sshweb.constant.ConstantPool;
import com.example.sshweb.pojo.WebSSHData;
import com.example.sshweb.service.WebSSHService;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.StreamCopier;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.sf.expectit.Expect;
import net.sf.expectit.ExpectBuilder;
import net.sf.expectit.Result;
import net.sf.expectit.matcher.Matchers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author: GuanBin
 * @date: Created in 上午10:31 2021/2/8
 */
@Component
public class WebSSHWebSocketHandler implements WebSocketHandler {

    @Autowired
    private WebSSHService webSSHService;
    private Logger logger = LoggerFactory.getLogger(WebSSHWebSocketHandler.class);


    private final SSHClient ssh = new SSHClient();
    private static final ExecutorService executorService = Executors.newCachedThreadPool();

    private WebSocketSession session;
    private Session.Shell shell;
    private Expect expect;

    private boolean sshConnected() {
        return ssh.isConnected() && ssh.isAuthenticated();
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
//        this.session = session;
        logger.info("用户:{},连接WebSSH", session.getAttributes().get(ConstantPool.USER_UUID_KEY));
        //调用初始化连接
        webSSHService.initConnection(session);

    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> webSocketMessage) throws Exception {

        if (webSocketMessage instanceof TextMessage) {
            logger.info("用户:{},发送命令:{}", session.getAttributes().get(ConstantPool.USER_UUID_KEY), webSocketMessage.toString());
            //调用service接收消息
            webSSHService.recvHandle(((TextMessage) webSocketMessage).getPayload(), session);
            recieveHandle(session,(TextMessage) webSocketMessage);

        } else if (webSocketMessage instanceof BinaryMessage) {

        } else if (webSocketMessage instanceof PongMessage) {

        } else {
            System.out.println("Unexpected WebSocket message type: " + webSocketMessage);
        }
    }

    private void recieveHandle(WebSocketSession session, TextMessage message) throws Exception {

        ObjectMapper objectMapper = new ObjectMapper();
        WebSSHData webSSHData = null;
        try {
            webSSHData = objectMapper.readValue(message.getPayload(), WebSSHData.class);
        } catch (IOException e) {
            logger.error("Json转换异常");
            logger.error("异常信息:{}", e.getMessage());
            return;
        }

        //连接sshServer
        if (!sshConnected()||ConstantPool.WEBSSH_OPERATE_CONNECT.equals(webSSHData.getOperate())) {
            checkSshString(message,webSSHData,session);
            return;
        }

        //连接sshServer后的操作
        executorService.submit(() -> {
            try {
                expect.sendLine(message.getPayload());
//                session.sendMessage(new TextMessage("\r"));
                Result result = expect.expect(Matchers.anyString());
                if (result.isSuccessful())
                    session.sendMessage(new TextMessage(result.getInput()));
                else
                    session.sendMessage(new TextMessage(message.getPayload() + " was unsuccessful.."));
            } catch (IOException e) {
                e.printStackTrace();
            }

        });
    }

    private void checkSshString(TextMessage message,WebSSHData webSSHData,WebSocketSession socketSession) throws Exception {
        String trim = message.getPayload();
//        if (trim.startsWith("connect::")) {
//
//            String replace = trim.replace("connect::", "");
//            String[] split = replace.split("\\|");
//            String user = split[0];
//            String host = split[1];
//            String port = split[2];
//            String password = split[3];

            connectToSshServer(webSSHData.getUsername(), webSSHData.getHost(), webSSHData.getPort(), webSSHData.getPassword(),socketSession);

//        }
    }

    private void connectToSshServer(String user, String host, Integer port, String password,WebSocketSession webSocketSession) throws Exception {
        ssh.loadKnownHosts();
        ssh.addHostKeyVerifier((hostname, p, key) -> true);
        ssh.connect(host, port);
        ssh.authPassword(user, password);
        Session session = ssh.startSession();
        session.allocateDefaultPTY();
        shell = session.startShell();
        expect = new ExpectBuilder()
                .withOutput(shell.getOutputStream())
                .withInputs(shell.getInputStream(), shell.getErrorStream())
                .withTimeout(5, TimeUnit.SECONDS)
                .build();



        expect.sendLine("> OK.\\n");
        Result result = expect.expect(Matchers.anyString());
        if (result.isSuccessful()) {
            webSocketSession.sendMessage(new TextMessage(result.getInput()));
        }

//        new StreamCopier(shell.getInputStream(), System.out).bufSize(shell.getLocalMaxPacketSize()).spawn("stdout");
//
//        new StreamCopier(shell.getErrorStream(), System.err).bufSize(shell.getLocalMaxPacketSize()).spawn("stderr");
//        new StreamCopier(System.in, shell.getOutputStream()).bufSize(shell.getRemoteMaxPacketSize()).copy();

//        webSocketSession.sendMessage(new TextMessage("connected::true"));


//        new StreamCopier(shell.getInputStream(), System.out, LoggerFactory.)
//                .bufSize(shell.getLocalMaxPacketSize())
//                .spawn("stdout");

        //        //读取终端返回的信息流
//        InputStream inputStream = shell.getInputStream();
//        try {
//            //循环读取
//            byte[] buffer = new byte[1024];
//            int i = 0;
//            //如果没有数据来，线程会一直阻塞在这个地方等待数据。
//            while ((i = inputStream.read(buffer)) != -1) {
//                session.sendMessage(new TextMessage(buffer));
//            }
//
//        }

                //读取终端返回的信息流
//        InputStream inputStream = shell.getInputStream();
//        try {
//            //循环读取
//            byte[] buffer = new byte[1024];
//            int i = 0;
//            //如果没有数据来，线程会一直阻塞在这个地方等待数据。
//            while ((i = inputStream.read(buffer)) != -1) {
//                session.sendMessage(new TextMessage(buffer));
//            }
//
//        }finally {
//            if (inputStream != null) {
//                inputStream.close();
//            }
//        }

//        session.sendMessage(new TextMessage(shell.getInputStream()));

//        //读取终端返回的信息流
//        InputStream inputStream = shell.getInputStream();
//        try {
//            //循环读取
//            byte[] buffer = new byte[1024];
//            int i = 0;
//            //如果没有数据来，线程会一直阻塞在这个地方等待数据。
//            while ((i = inputStream.read(buffer)) != -1) {
//                session.sendMessage(new TextMessage(buffer));
//            }
//
//        } finally {
//            //断开连接后关闭会话
//            ssh.disconnect();
//            shell.close();
//            if (inputStream != null) {
//                inputStream.close();
//            }
//        }

    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        System.out.println(status.getCode());
        System.out.println(status.getReason());

        webSSHService.close(session);
//        if (Objects.nonNull(ses))
//            ses.disconnect();
//        if (Objects.nonNull(channel))
//            channel.disconnect();
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        logger.error("数据传输错误");
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
}
