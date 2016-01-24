package com.myMoneyTracker.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.myMoneyTracker.dao.IncomeDao;
import com.myMoneyTracker.dao.UserRegistrationDao;
import com.myMoneyTracker.dto.income.IncomeDTO;
import com.myMoneyTracker.model.income.Income;
import com.myMoneyTracker.model.user.AppUser;
import com.myMoneyTracker.util.ControllerUtil;

/**
 * @author Floryn
 * Test class for the income controller
 */
@SuppressWarnings({"unchecked"})
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/spring-config.xml" })
public class IncomeControllerTest {

    private static final String LOGGED_USERNAME = "florin.e.iacob";

    @Autowired
    private IncomeController incomeController;

    @Autowired
    private AppUserController appUserController;

    @Autowired
    private IncomeDao incomeDao;
    
    @Autowired
    private UserRegistrationDao userRegistrationDao;

    @Before
    public void setup() {

        userRegistrationDao.deleteAll();
        userRegistrationDao.flush();
        incomeDao.deleteAll();
        incomeDao.flush();
        appUserController.deleteAll();
        ControllerUtil.setCurrentLoggedUser(LOGGED_USERNAME);

    }

    @Test
    public void shouldCreateIncome() {

        Income income = createIncome();
        AppUser appUser = createAppUser();
        ResponseEntity<?> responseEntity = appUserController.createAppUser(appUser);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        income.setUser(appUser);
        responseEntity = incomeController.createIncome(income);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertTrue(((IncomeDTO) responseEntity.getBody()).getId() > 0);
    }

    @Test
    public void shouldNotCreateIncome() {

        Income income = createIncome();
        AppUser appUser = createAppUser();
        ResponseEntity<?> responseEntity = appUserController.createAppUser(appUser);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        income.setUser(appUser);
        income.setName(null);
        responseEntity = incomeController.createIncome(income);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    }

    @Test
    public void shouldListAllIncomes() {

        Income income = createIncome();
        AppUser appUser = createAppUser();
        ResponseEntity<?> responseEntity = appUserController.createAppUser(appUser);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        income.setUser(appUser);
        responseEntity = incomeController.createIncome(income);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        responseEntity = incomeController.listAllIncomes();
        assertEquals(1, ((List<IncomeDTO>) responseEntity.getBody()).size());
        IncomeDTO result = ((List<IncomeDTO>) responseEntity.getBody()).get(0);
        assertEquals(income.getName(), result.getName());
    }

    @Test
    public void shouldFindById() {

        Income income = createIncome();
        AppUser appUser = createAppUser();
        ResponseEntity<?> responseEntity = appUserController.createAppUser(appUser);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        income.setUser(appUser);
        responseEntity = incomeController.createIncome(income);
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
        
        Income income = createAndSaveIncomeForAnotherUser();
        responseEntity = incomeController.findIncome(income.getId());
        assertEquals("Should not find another user's income!", HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    }

    @Test
    public void shouldUpdateIncome() {

        Income income = createIncome();
        AppUser appUser = createAppUser();
        ResponseEntity<?> responseEntity = appUserController.createAppUser(appUser);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        income.setUser(appUser);
        responseEntity = incomeController.createIncome(income);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        Income toUpdate = createIncome();
        toUpdate.setName("updated_income");
        toUpdate.setUser(appUser);
        responseEntity = incomeController.updateIncome(income.getId(), toUpdate);
        assertEquals(HttpStatus.NO_CONTENT, responseEntity.getStatusCode());

        responseEntity = incomeController.listAllIncomes();
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        List<IncomeDTO> found = (List<IncomeDTO>) responseEntity.getBody();
        assertEquals("updated_income", found.get(0).getName());
    }

    @Test
    public void shouldNotUpdateIncome() {
    
        Income income = createIncome();
        ResponseEntity<?> responseEntity = incomeController.updateIncome(111L, income);
        assertEquals("Should not update non-existent income!", HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
        
        income = createAndSaveIncomeForAnotherUser();
        responseEntity = incomeController.updateIncome(income.getId(), income);
        assertEquals("Should not update income for another user!", HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    }
    
    @Test
    public void shouldDeleteIncome() {

        Income income = createIncome();
        AppUser appUser = createAppUser();
        ResponseEntity<?> responseEntity = appUserController.createAppUser(appUser);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        income.setUser(appUser);
        responseEntity = incomeController.createIncome(income);
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
        
        Income income = createAndSaveIncomeForAnotherUser();
        responseEntity = incomeController.deleteIncome(income.getId());
        assertEquals("Should not delete income for another user!", HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    }
    
    @Test
    public void shouldDeleteAllIncomes() {

        Income income = createIncome();
        AppUser appUser = createAppUser();
        ResponseEntity<?> responseEntity = appUserController.createAppUser(appUser);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        income.setUser(appUser);
        responseEntity = incomeController.createIncome(income);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

        responseEntity = incomeController.deleteAll();
        assertEquals(HttpStatus.NO_CONTENT, responseEntity.getStatusCode());

        responseEntity = incomeController.listAllIncomes();
        assertEquals(HttpStatus.NO_CONTENT, responseEntity.getStatusCode());
    }

    private Income createIncome() {

        Income income = new Income();
        income.setName("name1");
        income.setDescription("description1");
        income.setAmount(new Double(222.222));
        income.setCreationDate(new Timestamp(System.currentTimeMillis()));
        return income;
    }

    private Income createAndSaveIncomeForAnotherUser() {
        
        AppUser anotherUser = createAppUser();
        anotherUser.setUsername("another_user");
        anotherUser.setEmail("another_user@email.com");
        appUserController.createAppUser(anotherUser);
        Income income = createIncome();
        income.setName("another_income");
        income.setUser(anotherUser);
        income = incomeDao.saveAndFlush(income);
        return income;
    }
    
    private AppUser createAppUser() {
    
        AppUser appUser = new AppUser();
        appUser.setFirstName("Florin");
        appUser.setSurname("Iacob");
        appUser.setPassword("TEST_PASS");
        appUser.setBirthdate(new Date());
        appUser.setUsername(LOGGED_USERNAME);
        appUser.setEmail("my-money-tracker@gmail.com");
        return appUser;
    }
}
