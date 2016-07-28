package com.duoshouji.server.user;

import org.jvnet.hk2.annotations.Contract;

import com.duoshouji.server.util.Password;

@Contract
public interface AccountSecurity {
	
	boolean verifyPassword(Password password);

	boolean hasPassword();

	void setPassword(Password password);
}
