/*
 * Copyright (C) 2012, Igor Ustyugov <igor@ustyugov.net>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/
 */

package net.ustyugov.jtalk.service;

import android.accounts.*;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.IBinder;
import net.ustyugov.jtalk.Notify;
import net.ustyugov.jtalk.activity.AddAccountActivity;
import net.ustyugov.jtalk.db.AccountDbHelper;
import net.ustyugov.jtalk.db.JTalkProvider;

public class AuthenticationService extends Service {

    private static AccountAuthenticatorImpl sAccountAuthenticator = null;

    @Override
    public IBinder onBind(Intent intent) {
        IBinder ret = null;
        if (intent.getAction().equals(android.accounts.AccountManager.ACTION_AUTHENTICATOR_INTENT)) {
            ret = getAuthenticator().getIBinder();
        }
        return ret;
    }

    private AccountAuthenticatorImpl getAuthenticator() {
        if (sAccountAuthenticator == null) {
            sAccountAuthenticator = new AccountAuthenticatorImpl(this);
        }
        return sAccountAuthenticator;
    }

    private static class AccountAuthenticatorImpl extends AbstractAccountAuthenticator {

        private Context mContext;

        public AccountAuthenticatorImpl(Context context) {
            super(context);
            mContext = context;
        }

        @Override
        public Bundle addAccount(AccountAuthenticatorResponse response, String accountType, String authTokenType, String[] requiredFeatures, Bundle options) throws NetworkErrorException {
            Intent i = new Intent(mContext, AddAccountActivity.class);
            i.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
            Bundle reply = new Bundle();
            reply.putParcelable(AccountManager.KEY_INTENT, i);
            return reply;
        }

        @Override
        public Bundle getAccountRemovalAllowed(AccountAuthenticatorResponse response, Account account) throws android.accounts.NetworkErrorException {
            String jid = account.name;
            Bundle result = super.getAccountRemovalAllowed(response, account);
            result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, true);

            Cursor cursor = mContext.getContentResolver().query(JTalkProvider.ACCOUNT_URI, null, AccountDbHelper.JID +" = '" + jid + "'", null, null);
            if (cursor != null && cursor.getCount() > 0) {
                cursor.moveToFirst();
                String id = cursor.getString(cursor.getColumnIndex(AccountDbHelper._ID));
                cursor.close();
                mContext.getContentResolver().delete(JTalkProvider.ACCOUNT_URI, "_id = '" + id + "'", null);
            }

            JTalkService service = JTalkService.getInstance();
            if (service.isAuthenticated(jid)) {
                service.disconnect(jid);
                if (service.isAuthenticated()) Notify.updateNotify();
                else Notify.offlineNotify(service.getGlobalState());
            }

            return result;
        }

        @Override
        public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) throws NetworkErrorException {
            return null;
        }

        @Override
        public Bundle hasFeatures(AccountAuthenticatorResponse arg0, Account arg1, String[] arg2) throws NetworkErrorException {
            return null;
        }

        @Override
        public Bundle updateCredentials(AccountAuthenticatorResponse arg0, Account arg1, String arg2, Bundle arg3) throws NetworkErrorException {
            return null;
        }

        @Override
        public String getAuthTokenLabel(String arg0) {
            return null;
        }

        @Override
        public Bundle confirmCredentials(AccountAuthenticatorResponse arg0, Account arg1, Bundle arg2) throws NetworkErrorException {
            return null;
        }

        @Override
        public Bundle editProperties(AccountAuthenticatorResponse arg0, String arg1) {
            return null;
        }
    }
}
