package com.sparta.myselectshop.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sparta.myselectshop.dto.SignupRequestDto;
import com.sparta.myselectshop.dto.UserInfoDto;
import com.sparta.myselectshop.entity.UserRoleEnum;
import com.sparta.myselectshop.jwt.JwtUtil;
import com.sparta.myselectshop.security.UserDetailsImpl;
import com.sparta.myselectshop.service.FolderService;
import com.sparta.myselectshop.service.KakaoService;
import com.sparta.myselectshop.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/api")
public class UserController {

    private final UserService userService;
    private final FolderService folderService;
    private final KakaoService kakaoService;

    @GetMapping("/user/login-page")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/user/signup")
    public String signupPage() {
        return "signup";
    }

    @PostMapping("/user/signup")
    public String signup(@Valid SignupRequestDto requestDto, BindingResult bindingResult) {
        // Validation 예외처리
        List<FieldError> fieldErrors = bindingResult.getFieldErrors();
        if (fieldErrors.size() > 0) {
            for (FieldError fieldError : bindingResult.getFieldErrors()) {
                log.error(fieldError.getField() + " 필드 : " + fieldError.getDefaultMessage());
            }
            return "redirect:/api/user/signup";
        }

        userService.signup(requestDto);

        return "redirect:/api/user/login-page";
    }

    // 회원 관련 정보 받기
    @GetMapping("/user-info")
    @ResponseBody
    public UserInfoDto getUserInfo(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        String username = userDetails.getUser().getUsername();
        UserRoleEnum role = userDetails.getUser().getRole();
        boolean isAdmin = (role == UserRoleEnum.ADMIN);

        return new UserInfoDto(username, isAdmin);
    }

    @GetMapping("/user-folder")
    public String getUserInfo(Model model, @AuthenticationPrincipal UserDetailsImpl userDetails) {
        model.addAttribute("folders", folderService.getFolders(userDetails.getUser()));
        //추가로 데이터(유저의 폴더 정보) 줘서 일정부분에 붙여넣기 위해 -> 타임리프 동적으로 처리!(model을 통해 전달)
        return "index :: #fragment";
    }

    // 1. 인가 코드 요청 (카카오로 로그인하기 버튼 클릭)

    // 사용자가 모든 필수 동의항목에 동의하고 [동의하고 계속하기] 버튼을 누른 경우
    //redirect_uri로 인가 코드를 담은 쿼리 스트링 전달
    //사용자가 동의 화면에서 [취소] 버튼을 눌러 로그인을 취소한 경우
    //redirect_uri로 에러 정보를 담은 쿼리 스트링 전달

    // 2. 인가 코드 전달 (카카오 -> Application) -> 리다이레트 uri(/api/user/kakao/callback) 호출

    // 3. 인가코드로 토큰 요청
    @GetMapping("/user/kakao/callback")
    public String kakaoLogin(@RequestParam String code, HttpServletResponse response) throws JsonProcessingException {
        String token = kakaoService.kakaoLogin(code);//4. JWT 반환
        //쿠키에 넣고->response에 반환
        Cookie cookie = new Cookie(JwtUtil.AUTHORIZATION_HEADER, token);
        cookie.setPath("/");
        response.addCookie(cookie);

        return "redirect:/";//로그인 성공 -> 메인페이지
    }
}