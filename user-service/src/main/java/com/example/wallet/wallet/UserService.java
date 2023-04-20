package com.example.wallet.wallet;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService implements UserDetailsService {

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    UserRepository userRepository;

    @Autowired
    KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    ObjectMapper objectMapper;

    @Override
    public User loadUserByUsername(String phoneNumber) throws UsernameNotFoundException {
        return userRepository.findByPhoneNumber(phoneNumber);
    }

    public void create(UserCreateRequest userCreateRequest) throws JsonProcessingException {
        User user = userCreateRequest.to();
        user.setPassword(encryptPwd(user.getPassword()));
        user.setAuthorities(UserConstants.USER_AUTHORITY);
        user = userRepository.save(user);

        // TODO: Publish the event post user creation which can be listened by consumers

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("userId", user.getId());
        jsonObject.put("phoneNumber", user.getPhoneNumber());
        jsonObject.put("identifierValue", user.getIdentifierValue());
        jsonObject.put("userIdentifier", user.getUserIdentifier());

        kafkaTemplate.send(CommonConstants.USER_CREATION_TOPIC,
                objectMapper.writeValueAsString(jsonObject));


    }

    public List<User> getAll() {
        return userRepository.findAll();
    }

    private String encryptPwd(String rawPwd){
        return passwordEncoder.encode(rawPwd);
    }
}
