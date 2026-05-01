package com.example.kserverproject.common.jwt;

import com.example.kserverproject.common.exception.enums.ErrorCode;
import com.example.kserverproject.common.exception.UserException;
import com.example.kserverproject.domain.user.entity.User;
import com.example.kserverproject.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow( () -> new UserException(ErrorCode.USER_NOT_FOUND));
        return new CustomUserDetails(user);
    }
}
