package Bespoke.BespokeBids.service;

import Bespoke.BespokeBids.domain.member.Member;
import Bespoke.BespokeBids.dto.ResponseDto;
import Bespoke.BespokeBids.dto.SignInDto;
import Bespoke.BespokeBids.dto.SignInResponseDto;
import Bespoke.BespokeBids.dto.SignUpDto;
import Bespoke.BespokeBids.repository.MemberRepository;
import Bespoke.BespokeBids.security.TokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final TokenProvider tokenProvider;
    private final FileService fileService;

    private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    
    public ResponseDto<?> signUp(SignUpDto dto, MultipartFile profilePicture) {
        String userId = dto.getUserId();
        String password = dto.getPassword();
        String passwordCheck = dto.getPasswordCheck();

        /**
         * 프로필 사진 업로드
         */
        String profilePictureUrl = null;
        if (profilePicture != null) {
            profilePictureUrl = fileService.saveProfilePicture(profilePicture);
            if (profilePictureUrl == null) {
                return ResponseDto.setFailed("Failed to upload profile picture");
            }
        }
        
        // id 중복 확인
        try {
            if (memberRepository.existsByUserId(userId)) {// 같은 아이디가 존재하면 true 없으면 false 반환
                return ResponseDto.setFailed("Existed Id!");
            }
        } catch (Exception e) {
            System.out.println("id 중복 확인 부분 오류 발생 ! : " + e);
            return ResponseDto.setFailed("Data base Error!");
        }


        /**
         * 비밀번호가 서로다르면 Failed 반환
         */
        if (!password.equals(passwordCheck)) {
            return ResponseDto.setFailed("Password does not matched!");
        }


        /**
         * member 저장
         */
        //비밀번호 인코딩
        dto.passwordEncoding(passwordEncoder.encode(password));

        try {
            memberRepository.save(new Member(dto));
        } catch (Exception e) {
            System.out.println("member 저장 부분 오류 발생! : " + e);
            return ResponseDto.setFailed("Data base Error!");
        }

        /**
         * 성공시 반환
         */
        return ResponseDto.setSuccess("Sign Up Success!!", null);





    }

    public ResponseDto<SignInResponseDto> signIn(SignInDto dto) {
        String userId = dto.getUserId();
        String userPassword = dto.getPassword();

        Member member = null;
        try {
            member = memberRepository.findByUserId(userId).get();
            //잘못된 아이디 일경우
            if (member == null) {
                return ResponseDto.setFailed("Sign In Failed!");
            }
            //비밀번호가 틀릴 경우
            if (!passwordEncoder.matches(userPassword, member.getPassword())) {
                return ResponseDto.setFailed("Sign In Failed!");
            }
        } catch (Exception e) {
            return ResponseDto.setFailed("DataBase Error!");
        }

        String token = tokenProvider.create(userId);
        Integer exprTime = 3600000;

        SignInResponseDto signInResponseDto = new SignInResponseDto(token, exprTime, member);
        return ResponseDto.setSuccess("Sign In Success", signInResponseDto);
    }
}

