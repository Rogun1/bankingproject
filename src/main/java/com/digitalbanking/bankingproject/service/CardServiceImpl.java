package com.digitalbanking.bankingproject.service;

import com.digitalbanking.bankingproject.config.CardNumberEncrypt;
import com.digitalbanking.bankingproject.constants.CardStatus;
import com.digitalbanking.bankingproject.dto.AccountRequestDTO;
import com.digitalbanking.bankingproject.dto.CardResponseDTO;
import com.digitalbanking.bankingproject.dto.CardStatusBlockRequestDTO;
import com.digitalbanking.bankingproject.model.Account;
import com.digitalbanking.bankingproject.model.Card;
import com.digitalbanking.bankingproject.model.Person;
import com.digitalbanking.bankingproject.repository.AccountRepository;
import com.digitalbanking.bankingproject.repository.CardRepository;
import com.digitalbanking.bankingproject.repository.PersonRepository;
import com.digitalbanking.bankingproject.service.declarations.CardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Random;

@Service
public class CardServiceImpl implements CardService {

    private final CardRepository cardRepository;
    private final AccountRepository accountRepository;
    private final PersonRepository personRepository;
    private final PasswordEncoder encoder;

    @Autowired
    public CardServiceImpl(
            CardRepository cardRepository,
            AccountRepository accountRepository,
            PersonRepository personRepository,
            PasswordEncoder encoder,
            CardNumberEncrypt encryption){

        this.cardRepository = cardRepository;
        this.accountRepository = accountRepository;
        this.personRepository = personRepository;
        this.encoder = encoder;
    }

    @Override
    public CardResponseDTO getCard(String email, AccountRequestDTO accountRequestDTO) throws Exception {

        Person person = personRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User doesn't exists for email: " + email));

        List<Account> accounts = accountRepository.findAllByPersonId(person.getId());
        Account account = null;

        for (Account acc: accounts){

            System.out.println("[" + acc.getCurrency() + "]");
            System.out.println("[" + accountRequestDTO.currency() + "]");

            if (acc.getCurrency().equals(accountRequestDTO.currency()) ) {
                Boolean existsCard = cardRepository.existsCardByAccountId(acc.getId());
                if (existsCard) {
                    throw new RuntimeException("You already have an card opened for: " + acc.getCurrency());
                }
                account = acc;
            }

        }
        if (account == null){
            throw new RuntimeException("No account found for currency: " + accountRequestDTO.currency());
        }

        String cardNumber = cardNumberGenerator();
        Integer cvvNumber = cvvGenerator();

        Integer year = LocalDate.now().getYear() + 7;
        Integer month = LocalDate.now().getMonthValue();
        Integer day = LocalDate.now().getDayOfMonth();
        LocalDate expDate = LocalDate.of(year,month,day);
        Integer dailyLimit = 5000;

        Card card = new Card(
                null,
                account,
                cardNumber,
                cvvNumber,
                expDate,
                CardStatus.ACTIVE,
                dailyLimit,
                new Date()
        );
        return toDTO(cardRepository.save(card));
    }

    @Override
    public CardResponseDTO blockCard(String email, Long cardId, CardStatusBlockRequestDTO cardStatus){
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new RuntimeException("Card not found for id: " + cardId));
        String cardStatusAsString = CardStatus.BLOCKED.toString();

        if (card.getStatus().toString().equals(cardStatusAsString)){
            throw new RuntimeException("Card is already blocked");
        }
        if (cardStatus.cardStatus().equals(cardStatusAsString)){
            card.setStatus(CardStatus.BLOCKED);
        }else{
            throw new RuntimeException("Invalid status to Block Card: " + cardStatus.cardStatus());
        }

        return toDTO(cardRepository.save(card));
    }


    //Simulating a card generator
    protected String cardNumberGenerator() throws Exception {
        Random random = new Random();
        String cardNumber = "";
        Boolean checkCardNumber = true;

        while (checkCardNumber){
            cardNumber = ""+ random.nextInt(1000, 9999)+
                    random.nextInt(1000, 9999)+
                    random.nextInt(1000, 9999)+
                    random.nextInt(1000, 9999);

            Long cardToCheck = Long.parseLong(cardNumber);
            Boolean existsCardNumber = cardRepository.existsByCardNumber(cardToCheck); // needs work with decrypt TO DO

            if (!existsCardNumber){
                checkCardNumber = false;
            }
        }
        return cardNumberEncryption(cardNumber);
    }

    //Simulating a CVV generator
    protected Integer cvvGenerator(){
        Random random = new Random();
        Integer cvvNumber = 0;
        Boolean checkCVVNumber = true;

        while (checkCVVNumber){
            cvvNumber = random.nextInt(100, 999);

            Boolean existsCardNumber = cardRepository.existsByCvv(cvvNumber);

            if (!existsCardNumber){
                checkCVVNumber = false;
            }
        }
        return cvvNumber;
    }

    //Card number encoding
    //Do a proper encoding/decoding TO DO
    protected String cardNumberEncryption(String cardNumber) throws Exception {

        String cardNumberString = cardNumber.toString();
        StringBuilder cardNmb = new StringBuilder(cardNumberString);

        String cardNumbersToEncrypt = cardNmb.substring(0,11);
        String cardNumbersRemained = cardNmb.substring(12,16);
//        SecretKey symmetricKey = CardNumberEncrypt.generateKey();
//        IvParameterSpec iv = CardNumberEncrypt.generateIv();

        // Just made to work, not a proper approach
        // Not actually needed to user encrypt for this app
//        String cipherText = CardNumberEncrypt.encrypt(
//                cardNumbersToEncrypt, symmetricKey, iv) + cardNumbersRemained;
        String beautyNumCode = "";

        for (int i = 0; i < cardNumbersToEncrypt.length() ; i++){
            beautyNumCode += "*";
        }
        beautyNumCode += cardNumbersRemained;
        return beautyNumCode;
    }
}
