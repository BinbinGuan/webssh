package com.example.sshweb.service.impl;

import com.example.sshweb.constant.ConstantPool;
import com.example.sshweb.pojo.SSHConnectInfo;
import com.example.sshweb.pojo.WebSSHData;
import com.example.sshweb.service.WebSSHService;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.IOUtils;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.Console;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author: GuanBin
 * @date: Created in 上午10:59 2021/2/8
 */
@Service
public class WebSSHServiceImpl2 implements WebSSHService {
    //存放ssh连接信息的map
    private static Map<String, Object> sshMap = new ConcurrentHashMap<>();

    private Logger logger = LoggerFactory.getLogger(WebSSHServiceImpl2.class);
    //线程池
    private ExecutorService executorService = Executors.newCachedThreadPool();

    private static final Console con = System.console();


    /**
     * @Description: 初始化连接
     * @Param: [session]
     * @return: void
     * @Author: NoCortY
     * @Date: 2020/3/7
     */
    @Override
    public void initConnection(WebSocketSession session) {
        final SSHClient sshClient = new SSHClient();
        SSHConnectInfo sshConnectInfo = new SSHConnectInfo();
        sshConnectInfo.setSshClient(sshClient);
        sshConnectInfo.setWebSocketSession(session);
        String uuid = String.valueOf(session.getAttributes().get(ConstantPool.USER_UUID_KEY));
        //将这个ssh连接信息放入map中
        sshMap.put(uuid, sshConnectInfo);
    }

    /**
     * @Description: 处理客户端发送的数据
     * @Param: [buffer, session]
     * @return: void
     * @Author: NoCortY
     * @Date: 2020/3/7
     */
    @Override
    public void recvHandle(String buffer, WebSocketSession session) {

//        ObjectMapper objectMapper = new ObjectMapper();
//        WebSSHData webSSHData = null;
//        try {
//            webSSHData = objectMapper.readValue(buffer, WebSSHData.class);
//        } catch (IOException e) {
//            logger.error("Json转换异常");
//            logger.error("异常信息:{}", e.getMessage());
//            return;
//        }
//        String userId = String.valueOf(session.getAttributes().get(ConstantPool.USER_UUID_KEY));
//        if (ConstantPool.WEBSSH_OPERATE_CONNECT.equals(webSSHData.getOperate())) {
//            //找到刚才存储的ssh连接对象
//            SSHConnectInfo2 SSHConnectInfo2 = (SSHConnectInfo2) sshMap.get(userId);
//            //启动线程异步处理
//            WebSSHData finalWebSSHData = webSSHData;
//            executorService.execute(new Runnable() {
//                @Override
//                public void run() {
//                    try {
//                        connectToSSH(SSHConnectInfo2, finalWebSSHData, session);
//                    } catch (JSchException | IOException e) {
//                        logger.error("webssh连接异常");
//                        logger.error("异常信息:{}", e.getMessage());
//                        close(session);
//                    }
//                }
//            });
//        } else if (ConstantPool.WEBSSH_OPERATE_COMMAND.equals(webSSHData.getOperate())) {
//            String command = webSSHData.getCommand();
//            SSHConnectInfo2 SSHConnectInfo2 = (SSHConnectInfo2) sshMap.get(userId);
//            if (SSHConnectInfo2 != null) {
//                try {
//                    transToSSH(SSHConnectInfo2.getSshClient(), command);
//                } catch (IOException e) {
//                    logger.error("webssh连接异常");
//                    logger.error("异常信息:{}", e.getMessage());
//                    close(session);
//                }
//            }
//        } else {
//            logger.error("不支持的操作");
//            close(session);
//        }
    }

    @Override
    public void sendMessage(WebSocketSession session, byte[] buffer) throws IOException {
        session.sendMessage(new TextMessage(buffer));
    }

    @Override
    public void close(WebSocketSession session) {
        String userId = String.valueOf(session.getAttributes().get(ConstantPool.USER_UUID_KEY));
        SSHConnectInfo SSHConnectInfo2 = (SSHConnectInfo) sshMap.get(userId);
        if (SSHConnectInfo2 != null) {
            //断开连接
            if (SSHConnectInfo2.getSshClient() != null) {
                try {
                    SSHConnectInfo2.getSshClient().disconnect();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            //map中移除
            sshMap.remove(userId);
        }
    }

//    /**
//     * @Description: 使用jsch连接终端
//     * @Param: [cloudSSH, webSSHData, webSocketSession]
//     * @return: void
//     * @Author: NoCortY
//     * @Date: 2020/3/7
//     */
//    private void connectToSSH(SSHConnectInfo2 sshConnectInfo2, WebSSHData webSSHData, WebSocketSession webSocketSession) throws JSchException, IOException {
////        Session session = null;
////        Properties config = new Properties();
////        config.put("StrictHostKeyChecking", "no");
////        //获取jsch的会话
////        session = SSHConnectInfo2.getjSch().getSession(webSSHData.getUsername(), webSSHData.getHost(), webSSHData.getPort());
////        session.setConfig(config);
////        //设置密码
////        session.setPassword(webSSHData.getPassword());
////        //连接  超时时间30s
////        session.connect(30000);
////
////        //开启shell通道
////        Channel channel = session.openChannel("shell");
////
////        //通道连接 超时时间3s
////        channel.connect(3000);
////
////        //设置channel
////        SSHConnectInfo2.setChannel(channel);
////
////        //转发消息
////        transToSSH(channel, "\r");
////
////        //读取终端返回的信息流
////        InputStream inputStream = channel.getInputStream();
////        try {
////            //循环读取
////            byte[] buffer = new byte[1024];
////            int i = 0;
////            //如果没有数据来，线程会一直阻塞在这个地方等待数据。
////            while ((i = inputStream.read(buffer)) != -1) {
////                sendMessage(webSocketSession, Arrays.copyOfRange(buffer, 0, i));
////            }
////
////        } finally {
////            //断开连接后关闭会话
////            session.disconnect();
////            channel.disconnect();
////            if (inputStream != null) {
////                inputStream.close();
////            }
////        }
//        SSHClient ssh = new SSHClient();
//        ssh.loadKnownHosts();
//        ssh.addHostKeyVerifier(new PromiscuousVerifier());
//        ssh.connect(webSSHData.getHost(), webSSHData.getPort());
//        ssh.authPassword(webSSHData.getUsername(), webSSHData.getPassword());
//        Session session = ssh.startSession();
//        Session.Shell shell = session.startShell();
//        sshConnectInfo2.setSshClient(ssh);
//        sshConnectInfo2.setSession(session);
//        transToSSH(ssh, "\r");
//        InputStream inputStream = shell.getInputStream();
//        try {
//            //循环读取
//            byte[] buffer = new byte[1024];
//            int i = 0;
//            //如果没有数据来，线程会一直阻塞在这个地方等待数据。
//            while ((i = inputStream.read(buffer)) != -1) {
//                sendMessage(webSocketSession, Arrays.copyOfRange(buffer, 0, i));
//            }
//
//        } finally {
//            //断开连接后关闭会话
//            session.close();
//            shell.close();
//            if (inputStream != null) {
//                inputStream.close();
//            }
//        }
//
//    }

//    /**
//     * @Description: 将消息转发到终端
//     * @Param: [channel, data]
//     * @return: void
//     * @Author: NoCortY
//     * @Date: 2020/3/7
//     */
//    private void transToSSH(Channel channel, String command) throws IOException {
//        if (channel != null) {
//            OutputStream outputStream = channel.getOutputStream();
//            outputStream.write(command.getBytes());
//            outputStream.flush();
//        }
//    }

    private void transToSSH(SSHClient sshClient, String command) throws IOException {
        if (sshClient != null) {
            Session.Command exec = sshClient.startSession().exec(command);
            System.out.println(IOUtils.readFully(exec.getInputStream()).toString());
            exec.join(5, TimeUnit.SECONDS);
            System.out.println(exec.getExitStatus());
        }
    }
}

