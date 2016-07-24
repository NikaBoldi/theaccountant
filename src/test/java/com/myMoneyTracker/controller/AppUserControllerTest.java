package com.myMoneyTracker.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Currency;
import java.util.Date;
import java.util.List;

import javax.validation.ConstraintViolationException;

import com.myMoneyTracker.dto.currency.DefaultCurrencyDTO;
import com.myMoneyTracker.util.PasswordEncrypt;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.myMoneyTracker.app.authentication.SessionAuthentication;
import com.myMoneyTracker.controller.exception.BadRequestException;
import com.myMoneyTracker.controller.exception.ConflictException;
import com.myMoneyTracker.controller.exception.NotFoundException;
import com.myMoneyTracker.dao.AppUserDao;
import com.myMoneyTracker.dao.AuthenticatedSessionDao;
import com.myMoneyTracker.dao.IncomeDao;
import com.myMoneyTracker.dao.UserRegistrationDao;
import com.myMoneyTracker.dto.user.AppUserDTO;
import com.myMoneyTracker.model.user.AppUser;
import com.myMoneyTracker.model.user.UserRegistration;
import com.myMoneyTracker.service.SessionService;

/**
 * @author Floryn
 *         Test class for the {@link AppUserController}
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/spring-config.xml" })
public class AppUserControllerTest {
    
    private String FIRST_NAME = "Floryn";
    private String username = "florin1234";
    
    @Autowired
    private AppUserController appUserController;
    
    @Autowired
    private IncomeDao incomeDao;
    
    @Autowired
    private AppUserDao appUserDao;
    
    @Autowired
    private UserRegistrationDao userRegistrationDao;
    
    @Autowired
    private AuthenticatedSessionDao authenticatedSessionDao;

    @Autowired
    private PasswordEncrypt passwordEncrypt;
    
    @Autowired
    private SessionService sessionService;
    
    @Before
    public void deleteAllUsers() {
        SecurityContextHolder.getContext().setAuthentication(new SessionAuthentication(username, "1.1.1.1"));
    }

    @After
    public void afterSetup(){
        SecurityContextHolder.getContext().setAuthentication(null);
    }
    
    @Test
    public void shouldCreateAppUser() {
    
        AppUser appUser = createAppUser(FIRST_NAME);
        ResponseEntity<?> responseEntity = appUserController.createAppUser(appUser);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        userRegistrationDao.deleteByUserId(((AppUserDTO) responseEntity.getBody()).getId());
        assertTrue(((AppUserDTO) responseEntity.getBody()).getId() > 0);
        appUserDao.delete(appUser.getId());
        appUserDao.flush();
    }
    
    @Test(expected = ConstraintViolationException.class)
    public void shouldNotCreateAppUserInvalidEmail() {
    
        AppUser appUser = createAppUser(FIRST_NAME);
        appUser.setEmail("wrongFormat");
        appUserController.createAppUser(appUser);
    }
    
    @Test(expected = BadRequestException.class)
    public void shouldNotCreateAppUserWithInvalidMail() {
    
        AppUser appUser = createAppUser(FIRST_NAME);
        appUser.setEmail("invalid_user@invalid_host.com");
        appUserController.createAppUser(appUser);
    }
    
    @Test
    public void shouldNotCreateDuplicateAppUser() {
    
        AppUser appUser = createAppUser(FIRST_NAME);
        appUserDao.save(appUser);
        long id = appUser.getId();
        appUser = createAppUser(FIRST_NAME);
        try {
            appUserController.createAppUser(appUser);
        } catch(Exception e){
            assertTrue(e instanceof ConflictException);
            appUserDao.delete(id);
            appUserDao.flush();
        }


    }
    
    @Test
    public void shouldFindOneUser() {
    
        AppUser appUser = createAppUser(FIRST_NAME);
        appUserDao.save(appUser);
        ResponseEntity<?> found = appUserController.findAppUser(appUser.getId());
        assertEquals(HttpStatus.OK, found.getStatusCode());
        assertTrue(found.getBody() != null);
        appUserDao.delete(appUser.getId());
        appUserDao.flush();
    }
    
    @Test(expected = NotFoundException.class)
    public void shouldNotFindOneUser() {
        appUserController.findAppUser(-1L);
    }
    
    @Test
    public void shouldUpdateUser() {
    
        AppUser appUser = createAppUser(FIRST_NAME);
        appUserDao.save(appUser);
        AppUser toUpdateAppUser = createAppUser("Florin");
        ResponseEntity<?> updated = appUserController.updateAppUser(appUser.getId(), toUpdateAppUser);
        assertEquals(HttpStatus.NO_CONTENT, updated.getStatusCode());
        assertEquals("User updated", updated.getBody());
        ResponseEntity<?> updatedUser = appUserController.findAppUser(appUser.getId());
        assertEquals("Florin", ((AppUserDTO) updatedUser.getBody()).getFirstName());
        userRegistrationDao.deleteByUserId(appUser.getId());
        appUserDao.delete(appUser.getId());
        appUserDao.flush();
    }
    
    @Test(expected = NotFoundException.class)
    public void shouldNotUpdateUser() {

        AppUser toUpdateAppUser = createAppUser("Florin");
        appUserController.updateAppUser(-1l, toUpdateAppUser);
    }
    
    @Test
    public void shouldRegisterAndActivateUser() {
    
        AppUser appUser = createAppUser(FIRST_NAME);
        appUserController.createAppUser(appUser);
        assertFalse("User should NOT be activated!", appUser.isActivated());
        List<UserRegistration> regList = userRegistrationDao.findByUserId(appUser.getId());
        assertFalse("Could not find userRegistration!", regList.isEmpty());
        appUserController.registerUser(regList.get(0).getCode());
        appUser = appUserDao.findOne(appUser.getId());
        assertTrue("User should be activated!", appUser.isActivated());
        userRegistrationDao.deleteByUserId(appUser.getId());
        appUserDao.delete(appUser.getId());
        appUserDao.flush();
    }

    @Test
    public void shouldLoginWithUsername() {

        AppUser appUser = createAppUser(FIRST_NAME);
        String uncryptedPassword = appUser.getPassword();
        String cryptedPassword = passwordEncrypt.encryptPassword(appUser.getPassword());
        appUser.setPassword(cryptedPassword);
        appUser.setActivated(true);
        String username = appUser.getUsername();
        appUserDao.save(appUser);

        String authorizationString = sessionService.encodeUsernameAndPassword(username, uncryptedPassword);
        ResponseEntity<?> loginResponseEntity = appUserController.login(authorizationString);
        assertEquals(HttpStatus.OK, loginResponseEntity.getStatusCode());
        userRegistrationDao.deleteByUserId(appUser.getId());
        appUserDao.delete(appUser.getId());
        appUserDao.flush();
    }

    @Test
    public void shouldSetAndGetDefaultCurrency() {

        String USD_CURRENCY = "USD";

        AppUser appUser = createAppUser(FIRST_NAME);
        String uncryptedPassword = appUser.getPassword();
        String cryptedPassword = passwordEncrypt.encryptPassword(appUser.getPassword());
        appUser.setPassword(cryptedPassword);
        appUser.setActivated(true);
        String username = appUser.getUsername();
        appUserDao.save(appUser);

        String authorizationString = sessionService.encodeUsernameAndPassword(username, uncryptedPassword);
        ResponseEntity<?> loginResponseEntity = appUserController.login(authorizationString);
        assertEquals(HttpStatus.OK, loginResponseEntity.getStatusCode());

        appUserController.setDefaultCurrency(new DefaultCurrencyDTO(USD_CURRENCY));

        ResponseEntity<?> response = appUserController.getDefaultCurrency();
        DefaultCurrencyDTO responseDTO = (DefaultCurrencyDTO) response.getBody();
        assertTrue("The Value Of the returned DefaultCurrency is invalid!", responseDTO.getValue().equals(USD_CURRENCY));

        userRegistrationDao.deleteByUserId(appUser.getId());
        appUserDao.delete(appUser.getId());
        appUserDao.flush();

    }

    @Test
    public void shouldNotSetDefaultCurrency() {

        String INVALID_CURRENCY = "INVALID";

        AppUser appUser = createAppUser(FIRST_NAME);
        String uncryptedPassword = appUser.getPassword();
        String cryptedPassword = passwordEncrypt.encryptPassword(appUser.getPassword());
        appUser.setPassword(cryptedPassword);
        appUser.setActivated(true);
        String username = appUser.getUsername();
        appUserDao.save(appUser);

        String authorizationString = sessionService.encodeUsernameAndPassword(username, uncryptedPassword);
        ResponseEntity<?> loginResponseEntity = appUserController.login(authorizationString);
        assertEquals(HttpStatus.OK, loginResponseEntity.getStatusCode());

        boolean exceptionThrown = false;
        try {
            appUserController.setDefaultCurrency(new DefaultCurrencyDTO(INVALID_CURRENCY));
        } catch (Exception e) {
            exceptionThrown = true;
            assertTrue("Setting invalid Currency should throw BadRequestException!", e instanceof BadRequestException);
        }

        assertTrue("Setting invalid Currency should throw Exception!", exceptionThrown == true);

        userRegistrationDao.deleteByUserId(appUser.getId());
        appUserDao.delete(appUser.getId());
        appUserDao.flush();
    }

    @Test
    public void shouldNotLoginNonActivatedUser() {
    
        AppUser appUser = createAppUser(FIRST_NAME);
        String username = appUser.getUsername();
        String password = appUser.getPassword();
        appUserDao.save(appUser);
        
        String authorizationString = sessionService.encodeUsernameAndPassword(username, password);
        try {
            appUserController.login(authorizationString);
        }catch(Exception e){
            assertTrue(e instanceof BadRequestException);
            appUserDao.delete(appUser.getId());
            appUserDao.flush();
        }

    }
    
    @Test
    public void shouldLoginWithEmail() {
    
        AppUser appUser = createAppUser(FIRST_NAME);
        String email = appUser.getEmail();
        String uncryptedPassword = appUser.getPassword();
        String password = passwordEncrypt.encryptPassword(appUser.getPassword());;
        appUser.setPassword(password);
        appUser.setActivated(true);
        appUserDao.save(appUser);
        
        String authorizationString = sessionService.encodeUsernameAndPassword(email, uncryptedPassword);
        ResponseEntity<?> loginResponseEntity = appUserController.login(authorizationString);
        assertEquals(HttpStatus.OK, loginResponseEntity.getStatusCode());
        appUserDao.delete(appUser.getId());
        appUserDao.flush();
    }
    
    @Test
    public void shouldNotLoginWrongUsername() {
    
        AppUser appUser = createAppUser(FIRST_NAME);
        String password = appUser.getPassword();
        appUserDao.save(appUser);
        
        String authorizationString = sessionService.encodeUsernameAndPassword("WrongUsername", password);
        try{
            appUserController.login(authorizationString);
        } catch(Exception e){
            assertTrue(e instanceof NotFoundException);
            appUserDao.delete(appUser.getId());
            appUserDao.flush();
        }

    }
    
    @Test
    public void shouldNotLoginIncorrectPassword() {
    
        AppUser appUser = createAppUser(FIRST_NAME);
        String username = appUser.getUsername();
        appUser.setActivated(true);
        appUserDao.save(appUser);

        String authorizationString = sessionService.encodeUsernameAndPassword(username, "wrong_pass");
        try{
            appUserController.login(authorizationString);
        }catch(Exception e){
            assertTrue(e instanceof BadRequestException);
            appUserDao.delete(appUser.getId());
            appUserDao.flush();
        }

    }
    
    @Test
    public void shouldNotLoginNullPassword() {
    
        AppUser appUser = createAppUser(FIRST_NAME);
        String username = appUser.getUsername();
        appUser.setActivated(true);
        appUserDao.save(appUser);

        String authorizationString = sessionService.encodeUsernameAndPassword(username, null);
        try{
            appUserController.login(authorizationString);
        } catch(Exception e){
            assertTrue(e instanceof BadRequestException);
            userRegistrationDao.deleteByUserId(appUser.getId());
            appUserDao.delete(appUser.getId());
            appUserDao.flush();
        }

    }
    
    @Test
    public void shouldLogoutUser() {
    
        AppUser appUser = createAppUser(FIRST_NAME);
        String uncryptedPassword = appUser.getPassword();
        String cryptedPassword = passwordEncrypt.encryptPassword(appUser.getPassword());
        String username = appUser.getUsername();
        appUser.setActivated(true);
        appUser.setPassword(cryptedPassword);
        appUserDao.save(appUser);

        String authorizationString = sessionService.encodeUsernameAndPassword(username, uncryptedPassword);
        ResponseEntity<?> loginResponseEntity = appUserController.login(authorizationString);
        assertEquals(HttpStatus.OK, loginResponseEntity.getStatusCode());
        
        ResponseEntity<?> logoutResponse = appUserController.logout(authorizationString);
        assertEquals(HttpStatus.OK, logoutResponse.getStatusCode());
        appUserDao.delete(appUser.getId());
        appUserDao.flush();
    }
    
    public void shouldNotLogoutUser() {
    
        String authorizationString = sessionService.encodeUsernameAndPassword(username, "invalid_password");
        ResponseEntity<?> logoutResponse = appUserController.logout(authorizationString);
        assertEquals(HttpStatus.BAD_REQUEST, logoutResponse.getStatusCode());
    }
    
    @Test(expected = BadRequestException.class)
    public void shouldNotRegisterAndActivateUser() {
    
        ResponseEntity<?> responseEntity = appUserController.registerUser("invalid_code");
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    }
    
    private AppUser createAppUser(String firstName) {
    
        AppUser appUser = new AppUser();
        appUser.setFirstName(firstName);
        appUser.setSurname("Grigoriu");
        appUser.setPassword("TEST_PASS");
        appUser.setUsername(username);
        appUser.setBirthdate(new Date());
        appUser.setEmail("my-money-tracker@gmail.com");
        appUser.setDefaultCurrency(Currency.getInstance("RON"));
        return appUser;
    }
}
