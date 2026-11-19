package com.minibank;

import com.minibank.dao.AccountDao.AccountDao;
import com.minibank.dao.ClientDao.ClientDao;
import com.minibank.dao.TransactionDao.TransactionDao;
import com.minibank.model.Account;
import com.minibank.model.Transaction;
import com.minibank.service.BankService;

import java.time.LocalDateTime;
import java.util.List;

public class Main {

    public static void main(String[] args) {


        BankService bank = new BankService(new ClientDao(),
                new AccountDao(),
                new TransactionDao());

//        Client client = bank.registerClient("Ivan Petro", "van@example.com");
//        Account account=bank.openAccount(client.getId(),"12345");

        Account account = bank.getAccount(199L);
        System.out.println(account);

//        bank.transfer(199L,203L,BigDecimal.valueOf(1000));


        System.out.println(bank.getAccount(199L));

        System.out.println(bank.getAccount(203L));

        List<Transaction> list = bank.getTransactions(203L, LocalDateTime.now(),
                LocalDateTime.now().plusDays(1));

        for (Transaction transaction : list) {
            System.out.println(transaction);
        }


//        System.out.println(bank.getClient(103L));


//


//        Account account1 = bank.openAccount(client.getId(), "ACC-001");
//        Account account2 = bank.openAccount(client.getId(), "ACC-002");

        // Пополняем account1 "вручную" переводом с самого себя не получится —
        // для демо просто задаём баланс напрямую через первый перевод
        // от несуществующего служебного счёта нет смысла эмулировать,
        // поэтому просто выставим баланс через пакетный доступ в демо:
//        account1.setBalance(new BigDecimal("1000.00"));
//
//        System.out.println("=== Обычный перевод ===");
//        bank.transfer(account1.getId(), account2.getId(), new BigDecimal("100.00"));
//        System.out.println("account1 balance: " + bank.getAccount(account1.getId()).getBalance());
//        System.out.println("account2 balance: " + bank.getAccount(account2.getId()).getBalance());
//
//        System.out.println();
//        System.out.println("=== Перевод на несуществующий счёт (баг) ===");
//        BigDecimal balanceBefore = bank.getAccount(account1.getId()).getBalance();
//        try {
//            bank.transfer(account1.getId(), 999L, new BigDecimal("50.00"));
//        } catch (IllegalArgumentException e) {
//            System.out.println("Ошибка (как и ожидалось): " + e.getMessage());
//        }
//        BigDecimal balanceAfter = bank.getAccount(account1.getId()).getBalance();
//        System.out.println("Баланс до попытки перевода: " + balanceBefore);
//        System.out.println("Баланс после попытки перевода: " + balanceAfter);
//        System.out.println("Деньги пропали: " + (balanceAfter.compareTo(balanceBefore) < 0));
    }
}
