package com.TheAccountant.controller;

import java.sql.Timestamp;
import java.util.Currency;
import java.util.Date;
import java.util.List;
import java.util.Random;

import com.TheAccountant.dao.AppUserDao;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.TheAccountant.dao.IncomeDao;
import com.TheAccountant.dto.income.IncomeDTO;
import com.TheAccountant.model.income.Income;
import com.TheAccountant.model.user.AppUser;
import com.TheAccountant.util.ControllerUtil;

import static org.junit.Assert.*;

/**
 * @author Tudor
 * Test class for the income controller
 */
@SuppressWarnings({"unchecked"})
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/spring-config.xml" })
@TestPropertySource(locations="classpath:application-test.properties")
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

        Income[] incomes = new Income[2];
        Income income1 = createIncome();
        income1.setUser(applicationUser);
        Income income2 = createIncome();
        income2.setUser(applicationUser);

        incomes[0] = income1;
        incomes[1] = income2;

        ResponseEntity<?> responseEntity = incomeController.createIncomes(incomes);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertTrue(((List<IncomeDTO>) responseEntity.getBody()).size() == 2);
    }

    @Test
    public void shouldNotCreateIncomeWithWrongCurrency() {

        Income[] incomes = new Income[1];
        Income income = createIncome();
        income.setUser(applicationUser);
        income.setCurrency("IAC");
        incomes[0] = income;

        ResponseEntity<?> responseEntity = incomeController.createIncomes(incomes);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertEquals("Wrong currency code for index [0] and Currency code [" + income.getCurrency() + "]!", responseEntity.getBody());
    }

    @Test
    public void shouldNotCreateIncome() {

        Income[] incomes = new Income[1];
        Income income = createIncome();
        income.setUser(applicationUser);
        income.setName(null);
        incomes[0] = income;

        boolean errorOccur = false;
        try {
            ResponseEntity<?> responseEntity = incomeController.createIncomes(incomes);
            assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        } catch(Throwable t) {
            errorOccur = true;
        }
        assertTrue("An error should occur while adding invalid Income!", errorOccur);
    }

    @Test
    public void shouldListAllIncomes() {

        Income[] incomes = new Income[1];
        Income income = createIncome();
        income.setUser(applicationUser);
        incomes[0] = income;
        ResponseEntity<?> responseEntity = incomeController.createIncomes(incomes);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

        responseEntity = incomeController.listAllIncomes();
        assertEquals(1, ((List<IncomeDTO>) responseEntity.getBody()).size());
        IncomeDTO result = ((List<IncomeDTO>) responseEntity.getBody()).get(0);
        assertEquals(income.getName(), result.getName());
    }

    @Test
    public void shouldFindById() {

        Income[] incomes = new Income[1];
        Income income = createIncome();
        income.setUser(applicationUser);
        incomes[0] = income;
        ResponseEntity<?> responseEntity = incomeController.createIncomes(incomes);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

        responseEntity = incomeController.findIncome(((List<IncomeDTO>) responseEntity.getBody()).get(0).getId());
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

        Income[] incomes = new Income[1];
        Income income = createIncome();
        income.setUser(applicationUser);
        incomes[0] = income;
        ResponseEntity<?> responseEntity = incomeController.createIncomes(incomes);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

        Income toUpdate = createIncome();
        toUpdate.setName("updated_income");
        toUpdate.setUser(applicationUser);
        responseEntity = incomeController.updateIncome(income.getId(), toUpdate);
        assertEquals(HttpStatus.NO_CONTENT, responseEntity.getStatusCode());

        Income found = incomeDao.findOne(income.getId());
        assertEquals("updated_income", found.getName());
    }

   @Test
    public void shouldNotUpdateIncomeWithWrongCurrency() {

        Income[] incomes = new Income[1];
        Income income = createIncome();
        income.setUser(applicationUser);
        incomes[0] = income;
        ResponseEntity<?> responseEntity = incomeController.createIncomes(incomes);
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

        Income[] incomes = new Income[1];
        Income income = createIncome();
        income.setUser(applicationUser);
        incomes[0] = income;
        ResponseEntity<?> responseEntity = incomeController.createIncomes(incomes);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

        responseEntity = incomeController.deleteIncome(income.getId());
        assertEquals(HttpStatus.NO_CONTENT, responseEntity.getStatusCode());

        Income found = incomeDao.findOne(income.getId());
        assertNull(found);
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

        Income[] incomes = new Income[1];
        Income income = createIncome();
        income.setUser(applicationUser);
        incomes[0] = income;
        ResponseEntity<?> responseEntity = incomeController.createIncomes(incomes);
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
