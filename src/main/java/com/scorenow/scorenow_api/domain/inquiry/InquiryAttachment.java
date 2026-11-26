package com.scorenow.scorenow_api.domain.inquiry;

import com.scorenow.scorenow_api.global.exception.BusinessException;
import com.scorenow.scorenow_api.global.exception.ErrorCode;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import org.springframework.web.multipart.MultipartFile;

record InquiryAttachment(String name, String contentType, byte[] data) {
    private static final byte[] PNG = {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};
    private static final byte[] OLE = {(byte) 0xd0, (byte) 0xcf, 0x11, (byte) 0xe0, (byte) 0xa1, (byte) 0xb1, 0x1a, (byte) 0xe1};

    static InquiryAttachment read(MultipartFile file) throws java.io.IOException {
        if (file.isEmpty() || file.getSize() > 5 * 1024 * 1024 || file.getOriginalFilename() == null)
            throw invalid();
        String original = file.getOriginalFilename().replace('\\', '/');
        String name = original.substring(original.lastIndexOf('/') + 1).trim();
        name = name.codePoints().filter(c -> !Character.isISOControl(c) && Character.getType(c) != Character.FORMAT)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString()
                .replaceAll("[<>:\"/\\\\|?*]", "_");
        int dot = name.lastIndexOf('.');
        if (name.length() > 180 || dot <= 0 || dot == name.length() - 1) throw invalid();
        String extension = name.substring(dot + 1).toLowerCase(Locale.ROOT);
        byte[] bytes = file.getBytes();
        String type;
        if ((extension.equals("jpg") || extension.equals("jpeg")) && bytes.length >= 3
                && (bytes[0] & 255) == 0xff && (bytes[1] & 255) == 0xd8 && (bytes[2] & 255) == 0xff) type = "image/jpeg";
        else if (extension.equals("png") && starts(bytes, PNG)) type = "image/png";
        else if (extension.equals("pdf") && starts(bytes, "%PDF-".getBytes(StandardCharsets.US_ASCII))) type = "application/pdf";
        else if (Set.of("zip", "docx", "xlsx", "pptx", "hwpx").contains(extension) && isZip(bytes)) type = "application/zip";
        else if (Set.of("doc", "xls", "ppt", "hwp").contains(extension) && starts(bytes, OLE)) type = "application/x-ole";
        else if (Set.of("txt", "csv").contains(extension) && isUtf8(bytes)) type = "text/plain";
        else throw invalid();
        return new InquiryAttachment(name, type, bytes);
    }

    boolean isImage() { return contentType.startsWith("image/"); }
    private static boolean starts(byte[] value, byte[] prefix) {
        return value.length >= prefix.length && Arrays.equals(value, 0, prefix.length, prefix, 0, prefix.length);
    }
    private static boolean isZip(byte[] value) {
        return value.length >= 4 && value[0] == 'P' && value[1] == 'K'
                && ((value[2] == 3 && value[3] == 4) || (value[2] == 5 && value[3] == 6) || (value[2] == 7 && value[3] == 8));
    }
    private static boolean isUtf8(byte[] value) {
        for (byte b : value) if (b == 0) return false;
        try { StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(value)); return true; }
        catch (CharacterCodingException e) { return false; }
    }
    private static BusinessException invalid() { return new BusinessException(ErrorCode.INVALID_PARAMETER); }
}
