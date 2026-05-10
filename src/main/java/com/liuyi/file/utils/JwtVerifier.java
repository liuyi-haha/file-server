package com.liuyi.file.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Data
@Component
public class JwtVerifier {

    @Value("${jwt.public-key-path}")
    private String publicKeyPath;

    private PublicKey publicKey;

    public static void main(String[] args) {
        JwtVerifier verifier = new JwtVerifier();
        verifier.setPublicKeyPath("config/public.pem");
        try {
            verifier.init();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        var claim = verifier.verify("eyJhbGciOiJSUzI1NiJ9.eyJ1c2VyX2lkIjoidXNlcjEyMyIsImZpbGVfaWQiOiJmaWxlNDU2IiwiaWF0IjoxNzc2MjI3ODY1LCJleHAiOjE3NzYyMjgxNjV9.UyWbzg0EAoU6mOfEIB9ktY3W0jIqSa2SgIg3Lq5l6z91dEqszTuuYwKWpX-BZvJ8TWMbO4gRdx2ROTTm_CQnDvcSXwxXLWX4DWkQeP3EXnnGnoY7BSabwajLzR7pEU91uecLyMxjiDtrZlZ_qYGjavmvnrCXbwzLdx6ugoQi_qPEbTpT8kcXPoLy0kioFHsw9Iy23VY8kqkVeh1-V8j33gozOsHbwqm_ysFmesTkwCG8zHVz43-MaO-pVETSj0l1gcXwob4P-2SMIxfvEsS9edmBj7hEHX_HTUE5FTojgkFWzaFR9Yjf7Hn2nidePC4_R8qySso7uRJFxMAP0Ln-ig");
        System.out.println(claim.get("user_id"));
        System.out.println(claim.get("file_id"));
    }

    @PostConstruct
    public void init() throws Exception {
        this.publicKey = loadPublicKey(publicKeyPath);
    }

    public Claims verify(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(publicKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private PublicKey loadPublicKey(String path) throws Exception {
        String content = new String(Files.readAllBytes(Paths.get(path)));
        content = content.replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        byte[] decoded = Base64.getDecoder().decode(content);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
        KeyFactory factory = KeyFactory.getInstance("RSA");
        return factory.generatePublic(spec);
    }
}