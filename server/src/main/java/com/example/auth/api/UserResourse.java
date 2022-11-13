package com.example.auth.api;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.example.auth.domain.AuthRole;
import com.example.auth.domain.AuthUser;
import com.example.auth.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@Slf4j
@RestController
@CrossOrigin("*")
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class UserResourse {
    private final UserService userService;

    @GetMapping("/users")
    public ResponseEntity<List<AuthUser>> getUser() {
        log.info("информация по всем пользователям была извлечена из базы данных");
        return ResponseEntity.ok().body(userService.getUsers());
    }
    @GetMapping("/perform_logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        String authorizationHeader = request.getHeader(AUTHORIZATION);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (authorizationHeader!= null && authorizationHeader.startsWith("Bearer ")) {
            try {
                String token = authorizationHeader.substring("Bearer ".length());
                Algorithm algorithm = Algorithm.HMAC256("secret".getBytes());
                JWTVerifier verifier = JWT.require(algorithm).build();
                DecodedJWT decodedJWT = verifier.verify(token);
                String username = decodedJWT.getSubject();
                AuthUser user = userService.getUser(username);
                HashMap<String, String> res = new HashMap<>();
                res.put("logout", "ok");
                new SecurityContextLogoutHandler().logout(request, response, auth);
                log.info("успешный выход из приложения");
                return new ResponseEntity<>(res, HttpStatus.OK);
            }
            catch (Exception exception) {
                log.error("Ошибка при выходе ииз приложения: {}", exception.getMessage());
                return new ResponseEntity<>(HttpStatus.FORBIDDEN);
            }
        }
        else {
            return new ResponseEntity<>("provide required token",HttpStatus.FORBIDDEN);
        }
    }

    @GetMapping("/user")
    public ResponseEntity<?> userInfo(HttpServletRequest request) {
        String authorizationHeader = request.getHeader(AUTHORIZATION);
        if (authorizationHeader!= null && authorizationHeader.startsWith("Bearer ")) {
            try {
                String token = authorizationHeader.substring("Bearer ".length());
                Algorithm algorithm = Algorithm.HMAC256("secret".getBytes());
                JWTVerifier verifier = JWT.require(algorithm).build();
                DecodedJWT decodedJWT = verifier.verify(token);
                String username = decodedJWT.getSubject();
                AuthUser user = userService.getUser(username);
//                Date exp = decodedJWT.getExpiresAt();
                HashMap<String, String> data = new HashMap<>();
                data.put("username", user.getUsername());
                data.put("email", user.getEmail());
                data.put("phone_number", user.getPhoneNumber());
                data.put("img", null);
                return new ResponseEntity<>(data, HttpStatus.OK);
            }
            catch (Exception exception) {
                log.error("Error logging in: {}", exception.getMessage());
                return new ResponseEntity<>(HttpStatus.FORBIDDEN);
            }
        }
        else {
            return new ResponseEntity<>("provide required token",HttpStatus.FORBIDDEN);
        }
    }
    @PostMapping("/user/save")
    public ResponseEntity<AuthUser> saveUser(@RequestBody AuthUser authUser) {
        userService.saveUser(authUser);
        userService.addRoleToUser(authUser.getUsername(), "ROLE_USER");
        log.info("пользователь с именем {} был добавлен в базу данных", authUser.getUsername());
        return ResponseEntity.ok().body(authUser);
    }

    @PostMapping("/streamer/save")
    public ResponseEntity<AuthUser> saveStreamer(@RequestBody AuthUser authUser) {
        userService.saveUser(authUser);
        userService.addRoleToUser(authUser.getUsername(), "ROLE_STEAMER");
        log.info("стример с именем {} был добавлен в базу данных", authUser.getUsername());
        return ResponseEntity.ok().body(authUser);
    }

    @PostMapping("/role/save")
    public ResponseEntity<AuthRole> saveUser(@RequestBody AuthRole authRole) {
        userService.saveRole(authRole);
        log.info("роль {} была добавлена в базу данных", authRole);
        return ResponseEntity.ok().body(authRole);
    }

    @PostMapping("/role/addroletouser")
    public ResponseEntity<?> addRoleToUser(@RequestBody RoleToUserForm form) {
        userService.addRoleToUser(form.getUsername(), form.getRoleName());
        log.info("пользователю {} была присвоена роль {}",form.getUsername(), form.getRoleName());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/token/refresh")
    public void refreshtoken(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String authorizationHeader = request.getHeader(AUTHORIZATION);
        if (authorizationHeader!= null && authorizationHeader.startsWith("Bearer ")) {
            try {
                String refresh_token = authorizationHeader.substring("Bearer ".length());
                Algorithm algorithm = Algorithm.HMAC256("secret".getBytes());
                JWTVerifier verifier = JWT.require(algorithm).build();
                DecodedJWT decodedJWT = verifier.verify(refresh_token);
                String username = decodedJWT.getSubject();
                AuthUser user = userService.getUser(username);
                String access_token = JWT.create()
                        .withSubject(user.getUsername())
                        .withExpiresAt(new Date(System.currentTimeMillis() + 10*60*1000))
                        .withIssuer(request.getRequestURL().toString())
                        .withClaim("roles", user.getRoles().stream().map(AuthRole::getName).collect(Collectors.toList()))
                        .sign(algorithm);
                Map<String,String> tokens = new HashMap<>();
                tokens.put("access_token",access_token);
                tokens.put("refresh_token",refresh_token);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                new ObjectMapper().writeValue(response.getOutputStream(), tokens);
            }
            catch (Exception exception) {
                response.setHeader("error", exception.getMessage());
                Map<String,String> error = new HashMap<>();
                error.put("access_token",exception.getMessage());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                new ObjectMapper().writeValue(response.getOutputStream(), error);
            }
        }
        else {
            throw new RuntimeException("Refresh token is missing");
        }
    }
}
