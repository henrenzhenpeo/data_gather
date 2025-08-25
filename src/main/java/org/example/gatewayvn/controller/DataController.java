package org.example.gatewayvn.controller;

import com.alibaba.fastjson.JSON;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsMessagingTemplate;
import org.springframework.messaging.MessagingException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.jms.Destination;
import javax.jms.Queue;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api")  // 统一API前缀
@Api(tags = "qms采集接口")
public class DataController {

//    @Value("${gateway.queue.target}")
//    private String targetQueue;  // 注入目标队列配置

    @Resource(name = "jmsMessageTemplate")
    private JmsMessagingTemplate jmsMessagingTemplate;

    @Resource(name = "AOI_J10-1_4")
    private Queue AOI;

    @Resource(name = "tianzunQueue")
    private Queue tianzunQueue;

    @Resource(name = "tianxingQueue")
    private Queue tianxingQueue;

    @Resource(name = "gzwdQueue")
    private Queue gzwdQueue;

    @Resource(name = "gzgtlQueue")
    private Queue gzgtlQueue;

    @Resource(name = "jptcjQueue")
    private Queue jptcjQueue;

    @Resource(name = "jptymQueue")
    private Queue jptymQueue;

    @Resource(name = "sdQueue")
    private Queue sdQueue;

    @Resource(name = "casiQueue")
    private Queue casiQueue;

    @Resource(name = "Report_ProductGague_J10-1_J10-1-4")
    private Queue reportBg;

    @Resource(name = "Report_ProductGague_A9-3_A9-3")
    private Queue reportCg;

    @Resource(name = "tkingQueue")
    private Queue tkingQueue;


    @Resource(name = "jtJmsMessagingTemplate")
    private JmsMessagingTemplate jtJmsMessagingTemplate; // 嘉泰 MQ

    @Resource(name = "jtQueue")
    private Queue jtQueue;

    /**
     * 博杰上传数据接口
     */
    @ApiOperation(value="博杰上传数据接口",notes = "博杰上传数据接口")
    @PostMapping("/User/UploadApi")
    public Map<String, Object> handleUserUpload(@RequestBody Object uploadData) {
        log.info("接收到博杰数据: {}", uploadData);
        return sendData(uploadData, AOI);
    }

    /**
     * 天准上传数据接口
     */
    @PostMapping("/tz/uploadApi")
    public Map<String, Object> handleTzUpload(@RequestBody Object uploadData) {
        log.info("接收到天准数据: {}", uploadData);
        return sendData(uploadData, tianzunQueue);
    }

    /**
     * 天行上传数据接口
     */
    @PostMapping("/tx/uploadApi")
    public Map<String, Object> handleTxUpload(@RequestBody Object uploadData) {
        log.info("接收到天行数据: {}", uploadData);

        return sendData(uploadData, tkingQueue);
    }

    /**
     * GZ551雾度 上传数据接口
     */
    @PostMapping("/gzwd/uploadApi")
    public Map<String, Object> handleGzWDUpload(@RequestBody Object uploadData) {
        log.info("接收到GZ551雾度数据: {}", uploadData);
        return sendData(uploadData, gzwdQueue);
    }

    /**
     * 广照GZ901S 透过率上传数据接口
     */
    @PostMapping("/gzgtl/uploadApi")
    public Map<String, Object> handleGzGTLUpload(@RequestBody Object uploadData) {
        log.info("接收到广照GZ901S透过率数据: {}", uploadData);
        return sendData(uploadData, gzgtlQueue);
    }

    /**
     * 捷普特抽检上传数据接口
     */
    @PostMapping("/jptcj/uploadApi")
    public Map<String, Object> handleJptCjUpload(@RequestBody Object uploadData) {
        log.info("接收到捷普特抽检数据: {}", uploadData);
        return sendData(uploadData, jptcjQueue);
    }

    /**
     * 捷普特油墨上传数据接口
     */
    @PostMapping("/jptym/uploadApi")
    public Map<String, Object> handleJptYmUpload(@RequestBody Object uploadData) {
        log.info("接收到捷普特油墨数据: {}", uploadData);
        return sendData(uploadData, jptymQueue);
    }

    /**
     * 晟鼎上传数据接口
     */
    @PostMapping("/sd/uploadApi")
    public Map<String, Object> handleSdUpload(@RequestBody Object uploadData) {
        log.info("接收到晟鼎数据: {}", uploadData);
        return sendData(uploadData, sdQueue);
    }

    /**
     * 中科慧远上传数据接口
     */
    @PostMapping("/casi/uploadApi")
    public Map<String, Object> handleCASIUpload(@RequestBody Object uploadData) {
        log.info("接收到中科慧远数据: {}", uploadData);
        return sendData(uploadData, casiQueue);
    }

    private Map<String, Object> sendData(Object uploadData, Queue queue) {
        Map<String, Object> resp = new HashMap<>();
        try {
            String jsonData = JSON.toJSONString(uploadData);
            log.info("准备发送数据: {}", jsonData);
            // 发到主MQ队列
            sendMessage(jmsMessagingTemplate, queue, jsonData);

            // 发到嘉泰MQ
            log.info("准备发送数据到jt通道: {}", jsonData);
            sendMessage(jtJmsMessagingTemplate, jtQueue, jsonData);

            resp.put("status_code", 1);
            resp.put("message", "数据上传成功");
        } catch (Exception e) {
            log.error("数据上传失败", e);
            resp.put("status_code", 0);
            resp.put("message", "数据上传失败: " + e.getMessage());
        }
        return resp;
    }

    // 发送消息，destination是发送到的队列，message是待发送的消息
    private void sendMessage(JmsMessagingTemplate template, Destination destination, final String message) {
        try {
            template.convertAndSend(destination, message);
            log.info("消息已发送到队列: {}", destination);
        } catch (MessagingException e) {
            log.error("发送信息失败", e);
        }
    }
}
