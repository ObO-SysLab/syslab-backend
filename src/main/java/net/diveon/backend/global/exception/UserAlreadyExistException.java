package net.diveon.backend.global.exception;


public class UserAlreadyExistException extends RuntimeException{
 
    public UserAlreadyExistException(){
        super("That User SignUp Unique ID is Already Exist");
    }
}
