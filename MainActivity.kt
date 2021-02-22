package com.netease.nmvideocreator

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.annotation.Nullable
import androidx.databinding.DataBindingUtil
import com.netease.appcommon.base.activity.ActivityBase
import com.netease.appcommon.mediapicker.util.PictureVideoScanner
import com.netease.appcommon.utils.SensorManagerHelper
import com.netease.appservice.network.NeteaseMusicConst
import com.netease.appservice.router.KRouter
import com.netease.appservice.router.RouterConst
import com.netease.appservice.router.RouterPath
import com.netease.cloudmusic.asynctask.NeteaseMusicAsyncTask
import com.netease.cloudmusic.common.ApplicationWrapper
import com.netease.cloudmusic.core.CoreUtils
import com.netease.cloudmusic.core.permission.OnPermissionCallback
import com.netease.cloudmusic.core.router.RouterRequest
import com.netease.cloudmusic.permission.PermissionDialogFragment
import com.netease.cloudmusic.utils.ToastHelper
import com.netease.cloudmusic.utils.isAppDebug
import com.netease.karaoke.LoginSession
import com.netease.nmvideocreator.app.mediapicker.BaseMaterialPickerActivity
import com.netease.nmvideocreator.databinding.ActivityMainBinding
import com.sankuai.waimai.router.annotation.RouterUri
import com.sankuai.waimai.router.components.UriSourceTools
import com.sankuai.waimai.router.core.UriRequest
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
ddddd
ffff
const val ACTION = "com.netease.nmvideocreator.HANDLE_AUTHORIZATION_RESPONSE"
const val USED_INTENT = "USED_INTENT"
const val LOG_TAG = "nmvideocreator"

@RouterUri(
    scheme = RouterConst.MINE_SCHEME,
    host = RouterConst.HOST,
    path = [RouterPath.MainActivity],
    priority = UriSourceTools.FROM_EXTERNAL
)
class MainActivity : ActivityBase() {
    private lateinit var mBinding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isAppDebug()) {
            SensorManagerHelper.getInstance(ApplicationWrapper.getInstance())
                .combindLifeCycleOwner(this)
        }
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        mBinding.dev.setOnClickListener {
            val uriRequest =
                UriRequest(this, RouterConst.getUri(listOf(RouterPath.DeveloperActivity)))
            uriRequest.setIntentFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            KRouter.route(uriRequest)
        }

        mBinding.importMaterial.setOnClickListener {
            PermissionDialogFragment.launch(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                object : OnPermissionCallback {
                    override fun onSuccess() {
                        val routerRequest = RouterRequest(
                            this@MainActivity, RouterConst.getUri(
                                listOf(RouterPath.MaterialPickerActivity)
                            )
                        )

                        routerRequest.putExtra(
                            BaseMaterialPickerActivity.EXTRA_TYPE,
                            PictureVideoScanner.Type.BOTH
                        )
                        KRouter.route(routerRequest)
                    }
                })
        }

        mBinding.importVideo.setOnClickListener {
            PermissionDialogFragment.launch(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                object : OnPermissionCallback {
                    override fun onSuccess() {
                        val routerRequest = RouterRequest(
                            this@MainActivity, RouterConst.getUri(
                                listOf(RouterPath.MaterialPickerActivity)
                            )
                        )

                        routerRequest.putExtra(
                            BaseMaterialPickerActivity.EXTRA_TYPE,
                            PictureVideoScanner.Type.VIDEO
                        )
                        KRouter.route(routerRequest)
                    }
                })
        }
        mBinding.importPicture.setOnClickListener {
            PermissionDialogFragment.launch(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                object : OnPermissionCallback {
                    override fun onSuccess() {
                        val routerRequest = RouterRequest(
                            this@MainActivity, RouterConst.getUri(
                                listOf(RouterPath.MaterialPickerActivity)
                            )
                        )

                        routerRequest.putExtra(
                            BaseMaterialPickerActivity.EXTRA_TYPE,
                            PictureVideoScanner.Type.IMAGE
                        )
                        KRouter.route(routerRequest)
                    }
                })
        }

        mBinding.findInspiration.setOnClickListener {
            val request = RouterRequest(
                this, RouterConst.getUri(listOf(RouterPath.FindInspirationActivity))
            )
            KRouter.route(request)
        }
        mBinding.videoEdit.setOnClickListener {
            val request = RouterRequest(
                this, RouterConst.getUri(listOf(RouterPath.EditActivity))
            )
            KRouter.route(request)
        }

        mBinding.login.setOnClickListener {
            val request = RouterRequest(this, RouterConst.getUri(listOf(RouterPath.LoginActivity)))
            KRouter.route(request)
        }

        mBinding.musicLibrary.setOnClickListener {
            val request =
                RouterRequest(this, RouterConst.getUri(listOf(RouterPath.MusicLibraryActivity)))
            KRouter.route(request)
        }

        mBinding.editBackground.setOnClickListener {
            val request =
                RouterRequest(this, RouterConst.getUri(listOf(RouterPath.EditBackgroundActivity)))
            KRouter.route(request)
        }



        ToastHelper.showToast("login:" + LoginSession.isLogined())
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        checkIntent(intent)
    }

    private fun checkIntent(@Nullable intent: Intent?) {
        if (intent != null) {
            when (intent.action) {
                ACTION -> if (!intent.hasExtra(
                        USED_INTENT
                    )
                ) {
                    handleAuthorizationResponse(intent)
                    intent.putExtra(USED_INTENT, true)
                }
                else -> {
                }
            }
        }
    }

    private fun handleAuthorizationResponse(intent: Intent) {
        val response = AuthorizationResponse.fromIntent(intent)
        val error = AuthorizationException.fromIntent(intent)
        val authState = AuthState(response, error)
        if (response != null) {
            Log.i(
                LOG_TAG,
                String.format("Handled Authorization Response %s ", authState.toString())
            )
            val service = AuthorizationService(this)
            service.performTokenRequest(
                response.createTokenExchangeRequest()
            ) { tokenResponse, exception ->
                if (exception != null) {
                    Log.w(LOG_TAG, "Token Exchange failed", exception)
                } else {
                    if (tokenResponse != null) {
                        authState.update(tokenResponse, exception)
//                        persistAuthState(authState)

                        // 认证成功，获得 access token
                        Log.i(
                            LOG_TAG,
                            String.format(
                                "Token Response [ Access Token: %s, ID Token: %s ]",
                                tokenResponse.accessToken,
                                tokenResponse.idToken
                            )
                        )

                        authState.performActionWithFreshTokens(
                            service
                        ) { accessToken, idToken, exception ->
                            object : NeteaseMusicAsyncTask<String?, Void?, JSONObject?>(this) {
                                override fun realDoInBackground(vararg tokens: String?): JSONObject? {
                                    Log.i(
                                        LOG_TAG,
                                        String.format("idToken: %s", idToken)
                                    )
                                    val client = OkHttpClient()
                                    val request: Request = Request.Builder()
                                        .url("https://login.netease.com/connect/userinfo")
                                        .addHeader(
                                            "Authorization",
                                            String.format("Bearer %s", tokens[0])
                                        )
                                        .build()
                                    try {
                                        // 成功获取 userinfo
                                        val response: Response = client.newCall(request).execute()
                                        val jsonBody: String? = response.body?.string()
                                        Log.i(
                                            LOG_TAG,
                                            String.format("User Info Response %s", jsonBody)
                                        )
                                        return JSONObject(jsonBody)
                                    } catch (exception: Exception) {
                                        Log.w(LOG_TAG, exception)
                                    }
                                    return null
                                }

                                override fun realOnPostExecute(result: JSONObject?) {
                                }
                            }.execute(accessToken)
                        }
                    }
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                NeteaseMusicConst.REQUEST_CODE.CHOOSE_SINGLE_PICTURE -> {
                    val pic = data?.getStringExtra("picture")
                    ToastHelper.showToast(pic)
                }
                NeteaseMusicConst.REQUEST_CODE.CHOOSE_PICTURE, NeteaseMusicConst.REQUEST_CODE.CHOOSE_VIDEO -> {
                    val pic = data?.getStringArrayListExtra("mediaData")?.joinToString(",")
                    ToastHelper.showToast(pic)
                }
                NeteaseMusicConst.REQUEST_CODE.CHOOSE_SINGLE_VIDEO -> {
                    val video = data?.getStringExtra("videoPath")
                    ToastHelper.showToast(video)
                }
            }
        }
    }
}
