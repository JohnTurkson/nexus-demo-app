package com.johnturkson.demo

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.johnturkson.common.client.Client
import com.johnturkson.common.model.CreateLinkinbioPostResponseData
import com.johnturkson.common.model.DeleteLinkinbioPostResponseData
import com.johnturkson.common.model.LinkinbioPost
import com.johnturkson.common.model.UpdateLinkinbioPostResponseData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    private var linkinbioPosts = emptyList<LinkinbioPost>()
    val userModel = NonNullMutableLiveData("1")
    val linkinbioPostsModel = NonNullMutableLiveData(linkinbioPosts)
    
    fun getLinkinbioPosts() {
        GlobalScope.launch(Dispatchers.IO) {
            linkinbioPosts = Client.getLinkinbioPosts(userModel.value)
            linkinbioPostsModel.postValue(linkinbioPosts.toList())
        }
    }
    
    fun subscribeToLinkinbioPostsUpdates() {
        GlobalScope.launch(Dispatchers.IO) {
            Client.connect()
            Client.subscribe(userModel.value)
            Client.updates.openSubscription().consumeAsFlow()
                .onEach { update -> println("update: $update") }
                .onEach { update ->
                    when (val data = update.data) {
                        is CreateLinkinbioPostResponseData -> addLinkinbioPost(data.data.linkinbioPost)
                        is UpdateLinkinbioPostResponseData -> updateLinkinbioPost(data.data.linkinbioPost)
                        is DeleteLinkinbioPostResponseData -> removeLinkinbioPost(data.data.linkinbioPost)
                    }
                }
                .collect()
        }
    }
    
    private fun addLinkinbioPost(linkinbioPost: LinkinbioPost) {
        linkinbioPosts = linkinbioPosts + linkinbioPost
        linkinbioPostsModel.postValue(linkinbioPosts)
    }
    
    private fun removeLinkinbioPost(linkinbioPost: LinkinbioPost) {
        linkinbioPosts = linkinbioPosts - linkinbioPost
        linkinbioPostsModel.postValue(linkinbioPosts)
    }
    
    private fun updateLinkinbioPost(linkinbioPost: LinkinbioPost) {
        linkinbioPostsModel.value
            .mapIndexed { index, post -> Pair(index, post) }
            .firstOrNull { (_, post) -> post.id == linkinbioPost.id }
            ?.let { (index, _) ->
                linkinbioPosts = linkinbioPosts.replace(index, linkinbioPost)
                linkinbioPostsModel.postValue(linkinbioPosts)
            }
    }
}

class NonNullMutableLiveData<T>(value: T) : MutableLiveData<T>(value) {
    override fun getValue(): T {
        return super.getValue()!!
    }
    
    override fun setValue(value: T) {
        super.setValue(value)
    }
    
    override fun postValue(value: T) {
        super.postValue(value)
    }
}
