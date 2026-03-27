package com.cooking.utils;

import cn.hutool.core.util.StrUtil;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailUtils {

    private static final String DEFAULT_SUBJECT = "邮箱验证码";

    private final JavaMailSender javaMailSender;

    @Value("${spring.mail.username}")
    private String from;

    @Value("classpath:/template/email_verification_code.html")
    private Resource verificationCodeTemplate;

    public void sendVerificationCodeEmail(String to, String code, Integer expireMinutes) {
        if (StrUtil.isBlank(to)) {
            throw new IllegalArgumentException("收件人邮箱不能为空");
        }
        if (StrUtil.isBlank(code)) {
            throw new IllegalArgumentException("验证码不能为空");
        }
        int effectiveMinutes = expireMinutes == null || expireMinutes <= 0 ? 5 : expireMinutes;
        String html = buildVerificationCodeHtml(code, effectiveMinutes);
        sendHtmlMail(to, DEFAULT_SUBJECT, html);
    }

    public void sendHtmlMail(String to, String subject, String htmlContent) {
        if (StrUtil.isBlank(from)) {
            throw new IllegalStateException("未配置发件人邮箱，请检查 spring.mail.username");
        }
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(StrUtil.blankToDefault(subject, DEFAULT_SUBJECT));
            helper.setText(htmlContent, true);
            javaMailSender.send(mimeMessage);
            log.info("邮件发送成功, to={}", to);
        } catch (MessagingException e) {
            log.error("邮件消息构建失败, to={}", to, e);
            throw new IllegalStateException("邮件消息构建失败", e);
        } catch (MailSendException e) {
            log.error("邮件发送失败, to={}", to, e);
            throw new IllegalStateException("邮件发送失败", e);
        }
    }

    private String buildVerificationCodeHtml(String code, int expireMinutes) {
        try {
            String template = StreamUtils.copyToString(
                    verificationCodeTemplate.getInputStream(),
                    StandardCharsets.UTF_8
            );
            return template
                    .replace("${code}", code)
                    .replace("${expireMinutes}", String.valueOf(expireMinutes));
        } catch (IOException e) {
            log.error("读取邮件模板失败");
            throw new IllegalStateException("读取邮件模板失败", e);
        }
    }
}
