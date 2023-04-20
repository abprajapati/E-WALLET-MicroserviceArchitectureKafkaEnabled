package com.example.wallet.wallet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class TxnService implements UserDetailsService {

    @Autowired
    TransactionRepository transactionRepository;

    @Autowired
    KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    ObjectMapper objectMapper;

    private static Logger logger = LoggerFactory.getLogger(TxnService.class);
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        JSONObject requestedUser = getUserFromUserService(username);

        List<GrantedAuthority> authorities;

        List<LinkedHashMap<String, String>> requestAuthorities = (List<LinkedHashMap<String, String>>)requestedUser.get("authorities");
        authorities = requestAuthorities
                .stream()
                .map(x -> x.get("authority"))
                .map(x -> new SimpleGrantedAuthority(x))
                .collect(Collectors.toList());

        return new User(
                (String)requestedUser.get("username"),
                (String)requestedUser.get("password"),
                authorities
        );


    }

    public String initiateTxn(String sender, String receiver, String purpose, Double amount) throws JsonProcessingException {
        logger.info("Inside initiateTxn method with sender - {}, receiver - {}, purpose - {}", sender, receiver, purpose);

        Transaction transaction = Transaction.builder()
                .sender(sender)
                .receiver(receiver)
                .purpose(purpose)
                .transactionId(UUID.randomUUID().toString())
                .transactionStatus(TransactionStatus.PENDING)
                .amount(amount)
                .build();

        transactionRepository.save(transaction);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("sender", sender);
        jsonObject.put("receiver", receiver);
        jsonObject.put("amount", amount);
        jsonObject.put("txnId", transaction.getTransactionId());

        kafkaTemplate.send(CommonConstants.TRANSACTION_CREATION_TOPIC, objectMapper.writeValueAsString(jsonObject));

        return transaction.getTransactionId();
    }

    @KafkaListener(topics = CommonConstants.WALLET_UPDATED_TOPIC, groupId = "grp123")
    public void updateTxn(String msg) throws ParseException, JsonProcessingException {
        JSONObject data = (JSONObject) new JSONParser().parse(msg);

        String txnId = (String) data.get("txnId");
        String sender = (String) data.get("sender");
        String receiver = (String) data.get("receiver");
        Double amount = (Double) data.get("amount");

        WalletUpdateStatus walletUpdateStatus = WalletUpdateStatus.valueOf((String)data.get("walletUpdateStatus"));

        JSONObject senderObj = getUserFromUserService(sender);
        String senderEmail = (String)senderObj.get("email");

        String receiverEmail = null;

        if(walletUpdateStatus == WalletUpdateStatus.SUCCESS){
            JSONObject receiverObj = getUserFromUserService(receiver);
            receiverEmail = (String)receiverObj.get("email");
            transactionRepository.updateTxn(txnId, TransactionStatus.SUCCESSFUL);
        }else{
            transactionRepository.updateTxn(txnId, TransactionStatus.FAILED);
        }

        String senderMsg = "Hi, your transaction with id " + txnId + " got " + walletUpdateStatus;

        JSONObject senderEmailObj = new JSONObject();
        senderEmailObj.put("email", senderEmail);
        senderEmailObj.put("msg", senderMsg);

        kafkaTemplate.send(CommonConstants.TRANSACTION_COMPLETED_TOPIC, objectMapper.writeValueAsString(senderEmailObj));

        if(walletUpdateStatus == WalletUpdateStatus.SUCCESS){
            String receiverMsg = "Hi, you have received Rs." + amount + " from "
                    + sender + " in your wallet linked with phone number " + receiver;
            JSONObject receiverEmailObj = new JSONObject();
            receiverEmailObj.put("email", receiverEmail);
            receiverEmailObj.put("msg", receiverMsg);

            kafkaTemplate.send(CommonConstants.TRANSACTION_COMPLETED_TOPIC, objectMapper.writeValueAsString(senderEmailObj));
        }
    }

    private JSONObject getUserFromUserService(String username){

        HttpHeaders httpHeaders = new HttpHeaders();

        httpHeaders.setBasicAuth("txn_service", "txn123");

        HttpEntity request = new HttpEntity(httpHeaders);

        return restTemplate.exchange("http://localhost:6001/admin/user/" + username,
                        HttpMethod.GET, request, JSONObject.class)
                .getBody();
    }
}
