package com.duoshouji.server.executor;

import com.duoshouji.server.util.VerificationCode;

public interface VerificationCodeAuthenticationExecutor {

	public static enum State {
		INIT,
		NOTIFIED,
		VERIFIED;
	}

	void sendVerificationCode();

	boolean verify(VerificationCode verificationCode);

	State getState();

}