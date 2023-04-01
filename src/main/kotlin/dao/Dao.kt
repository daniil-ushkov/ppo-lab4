package dao

import model.User
import model.Item
import rx.Observable

interface Dao {
    fun addUser(user: User): Observable<Boolean>
    fun getUser(id: Long): Observable<User>
    fun addItem(item: Item): Observable<Boolean>
    fun getItem(id: Long): Observable<Item>
}