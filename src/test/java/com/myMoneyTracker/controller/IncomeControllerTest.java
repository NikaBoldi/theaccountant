package com.myMoneyTracker.controller;

import java.sql.Timestamp;
import java.util.Currency;
import java.util.Date;
import java.util.List;
import java.util.Random;

import com.myMoneyTracker.dao.AppUserDao;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.myMoneyTracker.dao.IncomeDao;
import com.myMoneyTracker.dto.income.IncomeDTO;
import com.myMoneyTracker.model.income.Income;
import com.myMoneyTracker.model.user.AppUser;
import com.myMoneyTracker.util.ControllerUtil;

import static org.junit.Assert.*;

/**
 * @author Tudor
 * Test class for the income controller
 */
@SuppressWarnings({"unchecked"})
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/spring-config.xml" })
public class IncomeControllerTest {

    private static final String LOGGED_USERNAME = "florin.e.iacob";
    private AppUser applicationUser;
    
    @Autowired
    private IncomeController incomeController;

    @Autowired
    private IncomeDao incomeDao;

    @Autowired
    private AppUserDao appUserDao;

    @Before
    public void setup() {

        applicationUser = createAndSaveAppUser(LOGGED_USERNAME, "florin.iacob.expense@gmail.com");
        ControllerUtil.setCurrentLoggedUser(LOGGED_USERNAME);
    }

    @After
    public void cleanUp() {

        appUserDao.delete(applicationUser.getUserId());
        appUserDao.flush();

    }

    @Test
    public void shouldCreateIncome() {

        Income income = createIncome();
        income.setUser(applicationUser);
        ResponseEntity<?> responseEntity = incomeController.createIncome(income);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertTrue(((IncomeDTO) responseEntity.getBody()).getId() > 0);
    }

    @Test
    public void shouldNotCreateIncomeWithWrongCurrency() {

        Income income = createIncome();
        income.setUser(applicationUser);
        income.setCurrency("IAC");
        ResponseEntity<?> responseEntity = incomeController.createIncome(income);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals("Wrong currency code!", responseEntity.getBody());
    }

    @Test
    public void shouldNotCreateIncome() {

        Income income = createIncome();
        income.setUser(applicationUser);
        income.setName(null);
        ResponseEntity<?> responseEntity = incomeController.createIncome(income);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    }

    @Test
    public void shouldListAllIncomes() {

        Income income = createIncome();
        income.setUser(applicationUser);
        ResponseEntity<?> responseEntity = incomeController.createIncome(income);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        responseEntity = incomeController.listAllIncomes();
        assertEquals(1, ((List<IncomeDTO>) responseEntity.getBody()).size());
        IncomeDTO result = ((List<IncomeDTO>) responseEntity.getBody()).get(0);
        assertEquals(income.getName(), result.getName());
    }

    @Test
    public void shouldFindById() {

        Income income = createIncome();
        income.setUser(applicationUser);
        ResponseEntity<?> responseEntity = incomeController.createIncome(income);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        responseEntity = incomeController.findIncome(((IncomeDTO) responseEntity.getBody()).getId());
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        IncomeDTO found = (IncomeDTO) responseEntity.getBody();
        assertEquals(income.getName(), found.getName());
    }

    @Test
    public void shouldNotFindById() {

        ResponseEntity<?> responseEntity = incomeController.findIncome(new Random().nextLong());
        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
    }

    @Test
    public void shouldNotFindAnotherUserIncome(){
        Income income = createAndSaveIncomeForAnotherUser();
        ResponseEntity<?> responseEntity = incomeController.findIncome(income.getId());
        assertEquals("Should not find another user's income!", HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        appUserDao.delete(income.getUser().getUserId());
        appUserDao.flush();
    }

    @Test
    public void shouldUpdateIncome() {

        Income income = createIncome();
        income.setUser(applicationUser);
        ResponseEntity<?> responseEntity = incomeController.createIncome(income);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        Income toUpdate = createIncome();
        toUpdate.setName("updated_income");
        toUpdate.setUser(applicationUser);
        responseEntity = incomeController.updateIncome(income.getId(), toUpdate);
        assertEquals(HttpStatus.NO_CONTENT, responseEntity.getStatusCode());

        responseEntity = incomeController.listAllIncomes();
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        List<IncomeDTO> found = (List<IncomeDTO>) responseEntity.getBody();
        assertEquals("updated_income", found.get(0).getName());
    }

    @Test
    public void shouldNotUpdateIncomeWithWrongCurrency() {

        Income income = createIncome();
        income.setUser(applicationUser);
        ResponseEntity<?> responseEntity = incomeController.createIncome(income);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        Income toUpdate = createIncome();
        toUpdate.setName("updated_income");
        toUpdate.setCurrency("IAC");
        toUpdate.setUser(applicationUser);
        responseEntity = incomeController.updateIncome(income.getId(), toUpdate);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals("Wrong currency code!", (String) responseEntity.getBody());
    }

    @Test
    public void shouldNotUpdateIncome() {
    
        Income income = createIncome();
        ResponseEntity<?> responseEntity = incomeController.updateIncome(111L, income);
        assertEquals("Should not update non-existent income!", HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
    }

    @Test
    public void shouldNotUpdateOtherUserIncome(){

        Income income = createAndSaveIncomeForAnotherUser();
        ResponseEntity<?> responseEntity = incomeController.updateIncome(income.getId(), income);
        assertEquals("Should not update income for another user!", HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        appUserDao.delete(income.getUser().getUserId());
        appUserDao.flush();
    }
    
    @Test
    public void shouldDeleteIncome() {

        Income income = createIncome();
        income.setUser(applicationUser);
        ResponseEntity<?> responseEntity = incomeController.createIncome(income);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

        responseEntity = incomeController.deleteIncome(income.getId());
        assertEquals(HttpStatus.NO_CONTENT, responseEntity.getStatusCode());

        responseEntity = incomeController.findIncome(income.getId());
        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
    }

    @Test
    public void shouldNotDeleteIncome() {
    
        ResponseEntity<?> responseEntity = incomeController.deleteIncome(111L);
        assertEquals("Should not delete non-existent income!", HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
    }

    @Test
    public void shouldNotDeleteOtherUserIncome(){

        Income income = createAndSaveIncomeForAnotherUser();
        ResponseEntity<?> responseEntity = incomeController.deleteIncome(income.getId());
        assertEquals("Should not delete income for another user!", HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        appUserDao.delete(income.getUser().getUserId());
        appUserDao.flush();
    }
    
    @Test
    public void shouldDeleteAllIncomes() {

        Income income = createIncome();
        income.setUser(applicationUser);
        ResponseEntity<?> responseEntity = incomeController.createIncome(income);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

        responseEntity = incomeController.deleteAll();
        assertEquals(HttpStatus.NO_CONTENT, responseEntity.getStatusCode());

        responseEntity = incomeController.listAllIncomes();
        assertEquals(HttpStatus.NO_CONTENT, responseEntity.getStatusCode());
    }

    @Test
    public void shouldFindAllIncomesByInterval(){
        long fromTimestamp = System.currentTimeMillis();

        Income income = createIncome();
        income.setUser(applicationUser);
        incomeDao.save(income);

        Income incomeRON = createIncome();
        incomeRON.setUser(applicationUser);
        incomeRON.setCurrency("RON");
        incomeDao.save(incomeRON);

        long untilTimeStamp = System.currentTimeMillis();

        ResponseEntity responseEntity = incomeController.findByInterval(fromTimestamp, untilTimeStamp);
        assertEquals(2, ((List<IncomeDTO>) responseEntity.getBody()).size());
        IncomeDTO result = ((List<IncomeDTO>) responseEntity.getBody()).get(0);
        assertEquals("USD",result.getCurrency());
        assertNotNull(result.getDefaultCurrency());
        assertNotNull(result.getDefaultCurrencyAmount());
        result = ((List<IncomeDTO>) responseEntity.getBody()).get(1);
        assertEquals("RON",result.getCurrency());
        assertNull(result.getDefaultCurrency());
        assertNull(result.getDefaultCurrencyAmount());
    }

    private Income createIncome() {

        Income income = new Income();
        income.setName("name1");
        income.setDescription("description1");
        income.setCurrency("USD");
        income.setAmount(new Double(222.222));
        income.setCreationDate(new Timestamp(System.currentTimeMillis()));
        return income;
    }

    private Income createAndSaveIncomeForAnotherUser() {
        
        AppUser anotherUser = createAndSaveAppUser("another_user", "another_user@email.com");
        Income income = createIncome();
        income.setName("another_income");
        income.setCurrency("USD");
        income.setUser(anotherUser);
        income = incomeDao.saveAndFlush(income);
        return income;
    }
    
    private AppUser createAndSaveAppUser(String username, String email) {

        AppUser appUser = new AppUser();
        appUser.setFirstName("Florin");
        appUser.setSurname("Iacob");
        appUser.setPassword("TEST_PASS");
        appUser.setBirthdate(new Date());
        appUser.setUsername(username);
        appUser.setEmail(email);
        appUser.setDefaultCurrency(Currency.getInstance("RON"));
        appUser = appUserDao.saveAndFlush(appUser);
        return appUser;
    }
}
