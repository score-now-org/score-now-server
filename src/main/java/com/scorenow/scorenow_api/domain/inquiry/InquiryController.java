package com.scorenow.scorenow_api.domain.inquiry;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeVisitor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import com.scorenow.scorenow_api.domain.user.service.UserReferenceService;
import com.scorenow.scorenow_api.global.dto.ApiResponse;
import com.scorenow.scorenow_api.global.exception.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;

@RestController @RequiredArgsConstructor
public class InquiryController {
    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};
    private final InquiryRepository inquiries;
    private final InquiryImageRepository images;
    private final UserReferenceService users;
    public record Request(@NotBlank @Size(max = 200) String title, @NotBlank @Size(max = 10000) String content) {}
    public record Answer(@Size(max = 10000) String content, @Size(max = 10000) String contentHtml) {}
    public record FormattingCapabilities(boolean richFormatting) {}
    public record Response(Long id, String title, String content, String status, String answer,
                           String answerHtml, LocalDateTime createdAt, LocalDateTime answeredAt, boolean hasImage,
                           boolean hasAttachment, String attachmentName) {
        static Response of(Inquiry i) { return new Response(i.getId(), i.getTitle(), i.getContent(),
                i.getAnsweredAt() == null ? "WAITING" : "ANSWERED", i.getAnswer(), i.getAnswerHtml(), i.getCreatedAt(), i.getAnsweredAt(), i.isHasImage(), i.hasAttachment(), i.getAttachmentName()); }
    }
    public record AdminResponse(Long id, String title, String content, String status, String answer, String answerHtml,
                                LocalDateTime createdAt, LocalDateTime answeredAt, boolean hasImage, boolean hasAttachment,
                                String attachmentName, Long userId, String nickname) {
        static AdminResponse of(Inquiry i) { return new AdminResponse(i.getId(), i.getTitle(), i.getContent(),
                i.getAnsweredAt() == null ? "WAITING" : "ANSWERED", i.getAnswer(), i.getAnswerHtml(), i.getCreatedAt(),
                i.getAnsweredAt(), i.isHasImage(), i.hasAttachment(), i.getAttachmentName(), i.getUser().getId(), i.getUser().getNickname()); }
    }
    @PostMapping("/api/v1/inquiries") @Transactional
    public ApiResponse<Response> create(@AuthenticationPrincipal Long userId, @Valid @RequestBody Request request) {
        return ApiResponse.success(Response.of(inquiries.save(new Inquiry(users.requireActiveUser(userId), request.title(), request.content()))));
    }
    @GetMapping("/api/v1/inquiries") @Transactional(readOnly = true)
    public ApiResponse<Page<Response>> list(@AuthenticationPrincipal Long userId, @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "20") int size) {
        users.requireActiveUser(userId);
        return ApiResponse.success(inquiries.findByUser_Id(userId, pagination(page, size)).map(Response::of));
    }
    @GetMapping("/api/v1/inquiries/{id}") @Transactional(readOnly = true)
    public ApiResponse<Response> detail(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        users.requireActiveUser(userId);
        return ApiResponse.success(Response.of(inquiries.findByIdAndUser_Id(id, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND))));
    }
    @PutMapping("/api/v1/inquiries/{id}/image") @Transactional
    public ApiResponse<Void> uploadImage(@AuthenticationPrincipal Long userId, @PathVariable Long id,
                                         @RequestPart("image") MultipartFile image) throws IOException {
        Inquiry inquiry = lockOwned(id, userId);
        if (image.isEmpty() || image.getSize() > 5 * 1024 * 1024) throw new BusinessException(ErrorCode.INVALID_PARAMETER);
        byte[] bytes = image.getBytes();
        String type;
        if (bytes.length >= 3 && (bytes[0] & 255) == 0xff && (bytes[1] & 255) == 0xd8 && (bytes[2] & 255) == 0xff) type = "image/jpeg";
        else if (bytes.length >= 8 && java.util.Arrays.equals(bytes, 0, 8, PNG_SIGNATURE, 0, PNG_SIGNATURE.length)) type = "image/png";
        else throw new BusinessException(ErrorCode.INVALID_PARAMETER);
        images.save(new InquiryImage(id, type, bytes));
        inquiry.attachImage();
        return ApiResponse.success();
    }
    @PutMapping("/api/v1/inquiries/{id}/attachment") @Transactional
    public ApiResponse<Void> uploadAttachment(@AuthenticationPrincipal Long userId, @PathVariable Long id,
                                              @RequestPart("file") MultipartFile file) throws IOException {
        Inquiry inquiry = lockOwned(id, userId);
        InquiryAttachment attachment = InquiryAttachment.read(file);
        images.save(new InquiryImage(id, attachment.contentType(), attachment.data()));
        inquiry.attachFile(attachment.name(), attachment.isImage());
        return ApiResponse.success();
    }
    @GetMapping("/api/v1/inquiries/{id}/image") @Transactional(readOnly = true)
    public ResponseEntity<byte[]> image(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        users.requireActiveUser(userId);
        inquiries.findByIdAndUser_Id(id, userId).orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        return imageResponse(id, false);
    }
    @GetMapping("/api/v1/inquiries/{id}/image/download") @Transactional(readOnly = true)
    public ResponseEntity<byte[]> downloadImage(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        users.requireActiveUser(userId);
        inquiries.findByIdAndUser_Id(id, userId).orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        return imageResponse(id, true);
    }
    @GetMapping("/api/v1/inquiries/{id}/attachment") @Transactional(readOnly = true)
    public ResponseEntity<byte[]> downloadAttachment(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        users.requireActiveUser(userId);
        Inquiry inquiry = inquiries.findByIdAndUser_Id(id, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        return attachmentResponse(inquiry);
    }
    @GetMapping({"/admin/inquiries/{id}/attachment", "/api/v1/admin/inquiries/{id}/attachment"}) @Transactional(readOnly = true)
    public ResponseEntity<byte[]> adminAttachment(@PathVariable Long id) {
        return attachmentResponse(inquiries.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND)));
    }
    @GetMapping({"/admin/inquiries/{id}/image", "/api/v1/admin/inquiries/{id}/image"}) @Transactional(readOnly = true)
    public ResponseEntity<byte[]> adminImage(@PathVariable Long id) { return imageResponse(id, false); }
    @GetMapping({"/admin/inquiries/{id}", "/api/v1/admin/inquiries/{id}"}) @Transactional(readOnly = true)
    public ApiResponse<AdminResponse> adminDetail(@PathVariable Long id) {
        return ApiResponse.success(AdminResponse.of(inquiries.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND))));
    }
    @GetMapping({"/admin/inquiries/formatting-capabilities", "/api/v1/admin/inquiries/formatting-capabilities"})
    public ApiResponse<FormattingCapabilities> formattingCapabilities() {
        return ApiResponse.success(new FormattingCapabilities(true));
    }
    private ResponseEntity<byte[]> imageResponse(Long id, boolean download) {
        InquiryImage image = images.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        if (!"image/png".equals(image.getContentType()) && !"image/jpeg".equals(image.getContentType()))
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        var response = ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.parseMediaType(image.getContentType()))
                .header("X-Content-Type-Options", "nosniff");
        if (download) response.header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                .filename("inquiry-" + id + ("image/png".equals(image.getContentType()) ? ".png" : ".jpg")).build().toString());
        return response.body(image.getData());
    }
    private ResponseEntity<byte[]> attachmentResponse(Inquiry inquiry) {
        if (!inquiry.hasAttachment()) throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        InquiryImage file = images.findById(inquiry.getId()).orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        String name = inquiry.getAttachmentName();
        if (name == null) name = "inquiry-" + inquiry.getId() + ("image/png".equals(file.getContentType()) ? ".png" : ".jpg");
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(name, StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff").body(file.getData());
    }
    @GetMapping({"/admin/inquiries", "/api/v1/admin/inquiries"}) @Transactional(readOnly = true)
    public ApiResponse<Page<AdminResponse>> adminList(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String nickname, @RequestParam(required = false) Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String status) {
        if (LocalDate.MAX.equals(to) || (from != null && to != null && from.isAfter(to)) || (userId != null && userId < 1)
                || (status != null && !status.isBlank() && !status.equals("WAITING") && !status.equals("ANSWERED")))
            throw new BusinessException(ErrorCode.INVALID_PARAMETER);
        Specification<Inquiry> spec = (root, query, cb) -> {
            var conditions = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (nickname != null && !nickname.isBlank()) {
                String escaped = nickname.trim().toLowerCase(Locale.ROOT).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
                conditions.add(cb.like(cb.lower(root.get("user").get("nickname")), "%" + escaped + "%", '\\'));
            }
            if (userId != null) conditions.add(cb.equal(root.get("user").get("id"), userId));
            if (from != null) conditions.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from.atStartOfDay()));
            if (to != null) conditions.add(cb.lessThan(root.get("createdAt"), to.plusDays(1).atStartOfDay()));
            if ("WAITING".equals(status)) conditions.add(cb.isNull(root.get("answeredAt")));
            if ("ANSWERED".equals(status)) conditions.add(cb.isNotNull(root.get("answeredAt")));
            return cb.and(conditions.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        return ApiResponse.success(inquiries.findAll(spec, pagination(page, size)).map(AdminResponse::of));
    }
    @PutMapping({"/admin/inquiries/{id}/answer", "/api/v1/admin/inquiries/{id}/answer"}) @Transactional
    public ApiResponse<Response> answer(@PathVariable Long id, @Valid @RequestBody Answer request) {
        Inquiry inquiry = inquiries.findForUpdate(id).orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        String html = null;
        String text = request.content();
        if (request.contentHtml() != null) {
            html = sanitizeAnswer(request.contentHtml());
            text = plainText(html);
            if (text.isBlank() || text.length() > 10000) throw new BusinessException(ErrorCode.INVALID_PARAMETER);
        } else if (text == null || text.isBlank() || text.length() > 10000) throw new BusinessException(ErrorCode.INVALID_PARAMETER);
        inquiry.answer(text, html);
        return ApiResponse.success(Response.of(inquiry));
    }
    private static String sanitizeAnswer(String source) {
        Safelist allowed = Safelist.none().addTags("p", "div", "span", "br", "strong", "b", "em", "i", "u", "s", "strike", "ul", "ol", "li", "a", "table", "thead", "tbody", "tr", "th", "td")
                .addAttributes(":all", "style").addAttributes("a", "href").addProtocols("a", "href", "http", "https", "mailto");
        String clean = Jsoup.clean(source, "", allowed, new Document.OutputSettings().prettyPrint(false));
        Document document = Jsoup.parseBodyFragment(clean);
        document.outputSettings().prettyPrint(false);
        for (Element element : document.body().select("[style]")) {
            String style = safeStyle(element.attr("style"));
            if (style.isEmpty()) element.removeAttr("style"); else element.attr("style", style);
        }
        return document.body().html();
    }

    private static final Set<String> ANSWER_FONTS = Set.of("arial", "verdana", "georgia", "tahoma", "courier new", "malgun gothic");
    private static final Set<String> ANSWER_SIZES = Set.of("xx-small", "x-small", "small", "medium", "large", "x-large", "xx-large", "xxx-large");
    private static final Pattern RGB_COLOR = Pattern.compile("(?i)^rgb\\(\\s*(\\d{1,3})\\s*,\\s*(\\d{1,3})\\s*,\\s*(\\d{1,3})\\s*\\)$");

    private static String safeStyle(String source) {
        StringBuilder result = new StringBuilder();
        for (String declaration : source.split(";")) {
            String[] parts = declaration.split(":", 2);
            if (parts.length != 2) continue;
            String property = parts[0].trim().toLowerCase(Locale.ROOT);
            String value = parts[1].trim();
            String lower = value.toLowerCase(Locale.ROOT);
            String safe = switch (property) {
                case "font-family" -> {
                    String font = lower.replace("\"", "").replace("'", "").trim();
                    yield ANSWER_FONTS.contains(font) ? "'" + font + "'" : null;
                }
                case "font-size" -> ANSWER_SIZES.contains(lower) || lower.matches("(?:[8-9]|[1-6][0-9]|7[0-2])px") ? lower : null;
                case "font-weight" -> Set.of("normal", "bold", "100", "200", "300", "400", "500", "600", "700", "800", "900").contains(lower) ? lower : null;
                case "font-style" -> Set.of("normal", "italic").contains(lower) ? lower : null;
                case "text-decoration", "text-decoration-line" -> Set.of("none", "underline", "line-through", "underline line-through", "line-through underline").contains(lower) ? lower : null;
                case "text-align" -> Set.of("left", "center", "right", "justify").contains(lower) ? lower : null;
                case "color", "background-color" -> safeColor(lower);
                default -> null;
            };
            if (safe != null) result.append(property).append(": ").append(safe).append("; ");
        }
        return result.toString().trim();
    }

    private static String safeColor(String value) {
        if (value.matches("#[0-9a-f]{6}")) return value;
        var rgb = RGB_COLOR.matcher(value);
        if (!rgb.matches()) return null;
        int red = Integer.parseInt(rgb.group(1)), green = Integer.parseInt(rgb.group(2)), blue = Integer.parseInt(rgb.group(3));
        return red <= 255 && green <= 255 && blue <= 255 ? String.format("#%02x%02x%02x", red, green, blue) : null;
    }

    private static String plainText(String html) {
        var blockTags = Set.of("p", "div", "li", "tr");
        StringBuilder text = new StringBuilder();
        Jsoup.parse(html).body().traverse(new NodeVisitor() {
            private void line() { if (!text.isEmpty() && text.charAt(text.length() - 1) != '\n') text.append('\n'); }
            private boolean block(Node node) { return node instanceof Element element && blockTags.contains(element.normalName()); }
            @Override public void head(Node node, int depth) {
                if (block(node)) line();
                if (node instanceof TextNode value) text.append(value.getWholeText());
                else if (node instanceof Element element && element.normalName().equals("br")) text.append('\n');
            }
            @Override public void tail(Node node, int depth) {
                if (node instanceof Element element && Set.of("td", "th").contains(element.normalName()) && element.nextElementSibling() != null) text.append('\t');
                if (block(node)) line();
            }
        });
        return text.toString().trim();
    }
    private Inquiry lockOwned(Long id, Long userId) {
        // MySQL REPEATABLE READ snapshot must be established after acquiring this lock.
        Inquiry inquiry = inquiries.findOwnedForUpdate(id, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        users.requireActiveUser(userId);
        return inquiry;
    }
    private static Pageable pagination(int page, int size) {
        if (page < 0 || size < 1 || size > 100) throw new BusinessException(ErrorCode.INVALID_PARAMETER);
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
    }
}
