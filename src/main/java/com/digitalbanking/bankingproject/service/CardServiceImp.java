package com.digitalbanking.bankingproject.service;

import com.digitalbanking.bankingproject.constants.CardStatus;
import com.digitalbanking.bankingproject.dto.AccountRequestDTO;
import com.digitalbanking.bankingproject.dto.CardResponseDTO;
import com.digitalbanking.bankingproject.model.Account;
import com.digitalbanking.bankingproject.model.Card;
import com.digitalbanking.bankingproject.model.Customer;
import com.digitalbanking.bankingproject.repository.AccountRepository;
import com.digitalbanking.bankingproject.repository.CardRepository;
import com.digitalbanking.bankingproject.repository.CustomerRepository;
import com.digitalbanking.bankingproject.service.declarations.CardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Random;

@Service
public class CardServiceImp implements CardService {

    private final CardRepository cardRepository;
    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder encoder;

    @Autowired
    public CardServiceImp(
            CardRepository cardRepository,
            AccountRepository accountRepository,
            CustomerRepository customerRepository,
            PasswordEncoder encoder){

        this.cardRepository = cardRepository;
        this.accountRepository = accountRepository;
        this.customerRepository = customerRepository;
        this.encoder = encoder;
    }

    @Override
    public CardResponseDTO getCard(String email, AccountRequestDTO accountRequestDTO) {

        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Customer doesn't exists for email: " + email));

        List<Account> accounts = accountRepository.findAllByCustomerId(customer.getId());
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

        Long cardNumber = cardNumberGenerator();
        //hash all but last 4 -- To do
        //String cardEncoder = encoder.encode(cardNumber);
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

    //Simulating a card generator
    protected Long cardNumberGenerator(){
        Random random = new Random();
        String cardNumber = "";
        Boolean checkCardNumber = true;

        while (checkCardNumber){
            cardNumber = ""+ random.nextInt(1000, 9999)+
                    random.nextInt(1000, 9999)+
                    random.nextInt(1000, 9999)+
                    random.nextInt(1000, 9999);

            Long cardToCheck = Long.parseLong(cardNumber);
            Boolean existsCardNumber = cardRepository.existsByCardNumber(cardToCheck);

            if (!existsCardNumber){
                checkCardNumber = false;
            }
        }
        return Long.parseLong(cardNumber);
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
}
