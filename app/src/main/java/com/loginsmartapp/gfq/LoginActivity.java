package com.loginsmartapp.gfq;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.app.LoaderManager.LoaderCallbacks;

import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;

import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.Manifest.permission.INTERNET;
import static android.Manifest.permission.READ_CONTACTS;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AppCompatActivity {

    /**
     * ID para identificar la petición de permiso de Internet
     */
    private static final int REQUEST_INTERNET = 0;

    /**
     * Tarea Asincrona para iniciar sesión
     */
    private UserLoginTask mAuthTask = null;

    /**
     * referencias a los elementos de la interfaz
     */
    private EditText mEmailView;
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;

    private LoginActivity loginActivity = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //con esto se establece que la vista a utilizar es el layout con nombre de activity_login
        setContentView(R.layout.activity_login);

        //se asocia el elemento con id "email" del xml al objeto mEmailView
        mEmailView = (EditText) findViewById(R.id.email);
        //se comprueba que el acceso a internet este autorizado
        mayRequestInternet();
        //se asocia el elemento con id "password" del xml al objeto mPasswordView
        mPasswordView = (EditText) findViewById(R.id.password);
        //se le asocia el inicio de sesión al botón nombre "iniciar" que aparece el teclado de android
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    //funcción de comprobación e inicio de sesión
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });
        //se asocia el elemento con id "email_sing_in_button" del xml al objeto mEmailSignButton
        Button mEmailSignInButton = (Button) findViewById(R.id.email_sign_in_button);
        //crea un escuchar de eventos del tipo click, cuando un click sobre el boton se produce se llama a la función attemptLogin()
        mEmailSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        //se asocia el elemento con id "login_form" del xml al objeto mLoginFormView, este elemento representa al contenedor de los botones e input
        mLoginFormView = findViewById(R.id.login_form);
        //se asocia el elemento con id "login_progress" del xml al objeto mProgressView, es una barra de progreso circular que se verá mientras se inicia sesión
        mProgressView = findViewById(R.id.login_progress);
    }

    /**
     * con esta función se comprueba que los permisos se concedan o hayan sido concedidos
     */
    private boolean mayRequestInternet() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        if (checkSelfPermission(INTERNET) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        if (shouldShowRequestPermissionRationale(INTERNET)) {
            Snackbar.make(mEmailView, R.string.permission_rationale, Snackbar.LENGTH_INDEFINITE)
                    .setAction(android.R.string.ok, new View.OnClickListener() {
                        @Override
                        @TargetApi(Build.VERSION_CODES.M)
                        public void onClick(View v) {
                            requestPermissions(new String[]{INTERNET}, REQUEST_INTERNET);
                        }
                    });
        } else {
            requestPermissions(new String[]{INTERNET}, REQUEST_INTERNET);
        }
        return false;
    }

    /**
     * Se ejecuta cuando el usuario haya concedido o no los permisos
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_INTERNET) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mayRequestInternet();
            }
        }
    }


    /**
     * Esta función válida los datos y ejecuta la función de inicio de sesión
     */
    private void attemptLogin() {
        //si este tarea es distinta de null, significa que la sesión fue iniciada
        if (mAuthTask != null) {
            return;
        }

        //deparece los campos de error de los input, estos campos de error se muestran en rojo
        mEmailView.setError(null);
        mPasswordView.setError(null);

        //se obtiene el String correspondiente al input de email y contraseña
        String email = mEmailView.getText().toString();
        String password = mPasswordView.getText().toString();

        boolean cancel = false; // Valor lógico para efectuar o no el inicio de sesión
        View focusView = null; // este elemento sirve para centrar el "focus" sobre el elemento que tiene error


        //se comprueba que se haya escrito la contraseña
        if (TextUtils.isEmpty(password)) {
            mPasswordView.setError(getString(R.string.error_field_required));
            focusView = mPasswordView;
            cancel = true;
        }
        //se comprueba la validez del string de password
        if(!isPasswordValid(password)){
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }
        //se comprueba que se haya escrito el email
        if (TextUtils.isEmpty(email)) {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        }
        //se comprueba la validez del string de email
        if (!isEmailValid(email)) {
            mEmailView.setError(getString(R.string.error_invalid_email));
            focusView = mEmailView;
            cancel = true;
        }

        if (cancel) {
            //si hay errores, se centra el foco en el elemento con error
            focusView.requestFocus();
        } else {
            //sino, se muestra la barra circular de progreso y se inicia sesión
            showProgress(true);
            //se le entrega los string de email y contraseña a la tarea asincrona y se ejecuta
            mAuthTask = new UserLoginTask(email, password);
            mAuthTask.execute((Void) null);
        }
    }

    /**
     * función para validar el email
     */
    private boolean isEmailValid(String email) {
        return email.contains("@") && email.contains(".");
    }

    /**
     * función para validar el tamaño mínimo de un password
     */
    private boolean isPasswordValid(String password) {
        Log.d("PASSWORD", "tamaño " + password.length());
        return password.length() > 4;
    }

    /**
     * función para mostrar o esconder la barra de progreso circular
     * cuando se muestra la barra de progreso circular, desaparecen los input y el botón
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }


    /**
     * tarea asincrona que utiliza el ws de inicio de sesión
     */
    public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {

        private final String mEmail;
        private final String mPassword;

        UserLoginTask(String email, String password) {
            mEmail = email;
            mPassword = password;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            String url = "http://servicioswebmoviles.hol.es/index.php/LOGIN_UBB";
            RequestQueue queue = Volley.newRequestQueue(getApplication());

            StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            Log.d("JSON",response);
                            try{
                                JSONObject resp = new JSONObject(response);
                                if(resp.getBoolean("resp")){
                                    /*String saludo = "Hola " + resp.getJSONObject("data").getString("nombres") + resp.getJSONObject("data").getString("apellidos") + "!!";
                                    Snackbar bar = Snackbar.make(mEmailView, saludo, Snackbar.LENGTH_INDEFINITE);
                                    bar.setAction(R.string.snackbar_close, new OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            Toast toast = Toast.makeText(getApplication(), "Adios!!", Toast.LENGTH_LONG);
                                            toast.show();
                                        }
                                    });
                                    bar.show();*/
                                    Intent main_activity = new Intent(loginActivity, MainActivity.class);
                                    main_activity.putExtra("nombres",resp.getJSONObject("data").getString("nombres"));
                                    main_activity.putExtra("apellidos",resp.getJSONObject("data").getString("apellidos"));
                                    startActivity(main_activity);

                                } else if(resp.getString("info").equals("Contraseña Incorrecta")) {
                                    mPasswordView.setError(getString(R.string.error_incorrect_password));
                                    mPasswordView.requestFocus();
                                } else if(resp.getString("info").equals("No ha enviado datos")){
                                    Log.d("ERROR","sin datos");
                                }else{
                                    mEmailView.setError(getString(R.string.error_not_valid));
                                    mPasswordView.setError(getString(R.string.error_not_valid));
                                    mPasswordView.requestFocus();
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            mAuthTask = null;
                            showProgress(false);
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.d("JSON","Error");
                    mAuthTask = null;
                    showProgress(false);
                }
            }){
                @Override
                protected Map<String,String> getParams(){
                    Map<String,String> params = new HashMap<String, String>();
                    params.put("login", mEmail);
                    params.put("pass", mPassword);

                    return params;
                }
            };

            queue.add(stringRequest);
            return true;
        }


        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }
    }
}

