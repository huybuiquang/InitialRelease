package com.aionsigma.android.Model

import com.aionsigma.android.Constants.ConstSetting
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.security.cert.X509Certificate
import javax.net.ssl.*


class Repository {
    companion object {
        private var retrofit: Retrofit? = null
        private var builder: Retrofit.Builder = Retrofit.Builder()
                .baseUrl(ConstSetting.BASE_URL)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())

        private val httpClient = OkHttpClient.Builder()
        fun <S> createService(serviceClass: Class<S>): S {
            return createService(serviceClass, null)
        }

        fun <S> createService(serviceClass: Class<S>, authToken: Map<String, String>?): S {
            //begin unsafe OkHttpClient
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {

                }

                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {

                }

                override fun getAcceptedIssuers(): Array<X509Certificate> {
                    return arrayOf()
                }


            })

            // Install the all-trusting trust manager
            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, java.security.SecureRandom())

            // Create an ssl socket factory with our all-trusting manager
            val sslSocketFactory = sslContext.socketFactory

            httpClient.sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)

            httpClient.hostnameVerifier { hostname, session -> true }
            //end unsafe OkHttpClient



            if (authToken != null) {
                var interceptor = AuthenticationInterceptor(authToken!!)
                if (!httpClient.interceptors().contains(interceptor)) {
                    httpClient.addInterceptor(interceptor)
                    builder.client(httpClient.build())
                    retrofit = builder.build()
                }
            }
            else retrofit = builder.build()
            return retrofit!!.create(serviceClass)
        }


    }

    class AuthenticationInterceptor(private val authToken: Map<String, String>) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val original = chain.request()
            val builder = original.newBuilder()
            for (key in authToken.keys) {
                builder.header(key, authToken.getValue(key))
            }
            val request = builder.build()
            return chain.proceed(request)
        }
    }
}