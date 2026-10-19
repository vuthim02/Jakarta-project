package com.example.computer_store.controller.admin;

import com.example.computer_store.exception.NotFoundException;
import com.example.computer_store.exception.ValidationException;
import com.example.computer_store.model.MailMessage;
import com.example.computer_store.model.User;
import com.example.computer_store.service.MailService;
import com.example.computer_store.service.UserService;
import com.example.computer_store.util.Flash;
import com.example.computer_store.util.ValidationUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Gmail-style admin mailbox: inbox of customer messages, sent, compose and reply.
 */
@WebServlet({"/admin/mail", "/admin/mail/sent", "/admin/mail/compose", "/admin/mail/view", "/admin/mail/toggle", "/admin/mail/read-all"})
public class AdminMailServlet extends HttpServlet {

    private final MailService mailService = new MailService();
    private final UserService userService = new UserService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = sessionUser(request);
        switch (request.getServletPath()) {
            case "/admin/mail/sent" -> listMail(request, response, user, true);
            case "/admin/mail/compose" -> compose(request, response, user);
            case "/admin/mail/view" -> view(request, response, user);
            default -> listMail(request, response, user, false);
        }
    }

    private void listMail(HttpServletRequest request, HttpServletResponse response, User user, boolean sent)
            throws ServletException, IOException {
        if (sent) {
            request.setAttribute("messages", mailService.getSent(user.getUserId()));
            request.setAttribute("folder", "sent");
            request.getRequestDispatcher("/WEB-INF/views/admin/mail-sent.jsp").forward(request, response);
        } else {
            String filter = request.getParameter("filter");
            Boolean readOnly = "unread".equals(filter) ? Boolean.FALSE
                    : "read".equals(filter) ? Boolean.TRUE : null;
            request.setAttribute("filter", readOnly == null ? "all" : readOnly ? "read" : "unread");
            request.setAttribute("messages", mailService.getInbox(user.getUserId(), readOnly));
            int unread = mailService.countUnread(user.getUserId());
            request.setAttribute("unreadCount", unread);
            request.setAttribute("readCount", Math.max(0, mailService.getInbox(user.getUserId()).size() - unread));
            request.setAttribute("folder", "inbox");
            request.getRequestDispatcher("/WEB-INF/views/admin/mail-inbox.jsp").forward(request, response);
        }
    }

    private void compose(HttpServletRequest request, HttpServletResponse response, User user)
            throws ServletException, IOException {
        Integer replyTo = ValidationUtil.parseInt(request.getParameter("replyTo"));
        if (replyTo != null) {
            try {
                MailMessage original = mailService.getMessageForUser(replyTo, user.getUserId());
                request.setAttribute("replyTo", replyTo);
                request.setAttribute("original", original);
                request.setAttribute("replySubject", "Re: " + original.getSubject());
            } catch (NotFoundException e) {
                Flash.error(request, e.getMessage());
                response.sendRedirect(request.getContextPath() + "/admin/mail");
                return;
            }
        } else {
            request.setAttribute("recipients", userService.getCustomers());
        }
        request.getRequestDispatcher("/WEB-INF/views/admin/mail-compose.jsp").forward(request, response);
    }

    private void view(HttpServletRequest request, HttpServletResponse response, User user)
            throws ServletException, IOException {
        Integer id = ValidationUtil.parseInt(request.getParameter("id"));
        if (id == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        try {
            MailMessage message = mailService.getMessageForUser(id, user.getUserId());
            request.setAttribute("m", message);
            request.setAttribute("isRecipient", message.getRecipientId() == user.getUserId());
            request.setAttribute("profile", userService.get(message.getSenderId()));
            request.getRequestDispatcher("/WEB-INF/views/admin/mail-view.jsp").forward(request, response);
        } catch (NotFoundException e) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = sessionUser(request);
        String path = request.getServletPath();
        if ("/admin/mail/toggle".equals(path)) {
            Integer id = ValidationUtil.parseInt(request.getParameter("id"));
            if (id == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            try {
                MailMessage message = mailService.toggleRead(id, user.getUserId());
                response.sendRedirect(request.getContextPath() + "/admin/mail/view?id=" + message.getMessageId());
            } catch (NotFoundException e) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
            return;
        }
        if ("/admin/mail/read-all".equals(path)) {
            mailService.markAllRead(user.getUserId());
            Flash.success(request, "All messages marked as read.");
            response.sendRedirect(request.getContextPath() + "/admin/mail");
            return;
        }
        if (!"/admin/mail/compose".equals(path)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        Integer replyTo = ValidationUtil.parseInt(request.getParameter("replyTo"));
        try {
            MailMessage created;
            if (replyTo != null) {
                created = mailService.replyTo(user.getUserId(), replyTo, request.getParameter("body"));
                Flash.success(request, "Reply sent.");
                response.sendRedirect(request.getContextPath() + "/admin/mail/view?id=" + created.getMessageId());
            } else {
                Integer recipientId = ValidationUtil.parseInt(request.getParameter("recipientId"));
                if (recipientId == null) {
                    throw new ValidationException("Please choose a recipient.");
                }
                created = mailService.send(user.getUserId(), recipientId,
                        request.getParameter("subject"), request.getParameter("body"));
                Flash.success(request, "Message sent.");
                response.sendRedirect(request.getContextPath() + "/admin/mail/sent");
            }
        } catch (ValidationException | NotFoundException e) {
            Flash.error(request, e.getMessage());
            String back = replyTo != null ? "/admin/mail/compose?replyTo=" + replyTo : "/admin/mail/compose";
            response.sendRedirect(request.getContextPath() + back);
        }
    }

    private User sessionUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null ? null : (User) session.getAttribute("user");
    }
}