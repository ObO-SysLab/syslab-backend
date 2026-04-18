package net.diveon.backend.global.exception;


/**<pre>
 * 유저 가입시, 아이디가 중복된 경우 생성이 불가능하다
 * 해당 상황시 throw 되는 exception 객체
 * </pre>
 * @param no parameter
 * @return no return
 */
public class UserAlreadyExistException extends RuntimeException{
 
    public UserAlreadyExistException(){
        super("That User SignUp Unique ID is Already Exist");
    }
}
