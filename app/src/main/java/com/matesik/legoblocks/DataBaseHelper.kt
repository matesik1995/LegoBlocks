package com.matesik.legoblocks

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import android.graphics.Bitmap.CompressFormat
import android.R.attr.bitmap
import java.io.ByteArrayOutputStream


data class Project(
        val id: Int,
        val name: String,
        val active: Int,
        val lastAccessed: Int)

data class InventoryPart(
        val inventoryId: Int,
        val typeId: Int,
        val itemId: Int,
        val qtyInSet: Int,
        val qtyInStore: Int,
        val colorId: Int,
        val extra: Int)

class DataBaseHelper(private val myContext: Context) : SQLiteOpenHelper(myContext, DB_NAME, null, 1) {

    companion object {
        private val DB_PATH = "/data/data/" + "com.matesik.legoblocks" + "/databases/"
        private val DB_NAME = "BrickList.db"
    }

    object TABLE {
        val INVENTORIES = "Inventories"
        val INVENTORIES_PARTS = "InventoriesParts"
        val ITEM_TYPES = "ItemTypes"
        val PARTS = "Parts"
        val CODES = "Codes"
    }

    object FIELD {
        val ID = "_id"
        val NAME = "Name"
        val ACTIVE = "Active"
        val LAST_ACCESSED = "LastAccessed"
        val INVENTORY_ID = "InventoryID"
        val TYPE_ID = "TypeID"
        val ITEM_ID = "ItemID"
        val QTY_IN_SET = "QuantityInSet"
        val QTY_IN_STORE = "QuantityInStore"
        val COLOR_ID = "ColorID"
        val EXTRA = "Extra"
        val CODE = "Code"
        val IMAGE = "Image"
    }

    fun createDataBase() {
        try {
            SQLiteDatabase.openDatabase(DB_PATH + DB_NAME, null, SQLiteDatabase.OPEN_READONLY).close();
        } catch (e: SQLiteException) {
            this.readableDatabase
            try {
                val myInput = myContext.getAssets().open(DB_NAME)
                val outFileName = DB_PATH + DB_NAME
                val myOutput = FileOutputStream(outFileName)
                val buffer = ByteArray(1024)
                var length: Int = myInput.read(buffer)
                while (length > 0) {
                    myOutput.write(buffer, 0, length)
                    length = myInput.read(buffer)
                }
                myOutput.flush()
                myOutput.close()
                myInput.close()
            } catch (e: IOException) {
                throw Error("Error copying database")
            }
        }
    }

    fun deleteDB() {
        File(DB_PATH + DB_NAME).delete()
    }

    override fun onCreate(db: SQLiteDatabase) {
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    }

    //  `Inventoreis` table related methods
    fun getProject(id: Int): Project? {
        val query = "SELECT * FROM ${TABLE.INVENTORIES} WHERE ${FIELD.ID} = $id"
        val db = this.readableDatabase
        val cursor = db.rawQuery(query, null)
        var project : Project? = null
        if(cursor.moveToFirst()) {
             project = Project(
                    id = cursor.getInt(cursor.getColumnIndex(FIELD.ID)),
                    name = cursor.getString(cursor.getColumnIndex(FIELD.NAME)),
                    active = cursor.getInt(cursor.getColumnIndex(FIELD.ACTIVE)),
                    lastAccessed = cursor.getInt(cursor.getColumnIndex(FIELD.LAST_ACCESSED))
            )
        }
        cursor.close()
        db.close()
        return project
    }

    fun fetchProjects(): ArrayList<Project> {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM ${TABLE.INVENTORIES}", null)
        val projects = ArrayList<Project>()
        if (cursor.moveToFirst()) {
            do {
                projects.add(Project(
                        id = cursor.getInt(cursor.getColumnIndex(FIELD.ID)),
                        name = cursor.getString(cursor.getColumnIndex(FIELD.NAME)),
                        active = cursor.getInt(cursor.getColumnIndex(FIELD.ACTIVE)),
                        lastAccessed = cursor.getInt(cursor.getColumnIndex(FIELD.LAST_ACCESSED))
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return projects
    }

    fun insertProject(project: Project) {
        val values = ContentValues()
        values.put(FIELD.ID, project.id)
        values.put(FIELD.NAME, project.name)
        values.put(FIELD.ACTIVE, project.active)
        values.put(FIELD.LAST_ACCESSED, project.lastAccessed)
        val db = this.writableDatabase
        db.insert(TABLE.INVENTORIES, null, values)
        db.close()
    }

    fun updateProject(project: Project) {
        val values = ContentValues()
        values.put(FIELD.NAME, project.name)
        values.put(FIELD.ACTIVE, project.active)
        values.put(FIELD.LAST_ACCESSED, project.lastAccessed)
        val db = this.writableDatabase
        db.update(TABLE.INVENTORIES, values, "${FIELD.ID} = ?", arrayOf(project.id.toString()))
        db.close()
    }

    // `InventoriesPrats` table related methods
    fun fetchInventoryParts(projectId: Int): ArrayList<InventoryPart> {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM ${TABLE.INVENTORIES_PARTS} WHERE ${FIELD.INVENTORY_ID} = ?", arrayOf(projectId.toString()))
        val parts = ArrayList<InventoryPart>()
        if (cursor.moveToFirst()) {
            do {
                parts.add(InventoryPart(
                        inventoryId = cursor.getInt(cursor.getColumnIndex(FIELD.INVENTORY_ID)),
                        typeId = cursor.getInt(cursor.getColumnIndex(FIELD.TYPE_ID)),
                        itemId = cursor.getInt(cursor.getColumnIndex(FIELD.ITEM_ID)),
                        qtyInSet = cursor.getInt(cursor.getColumnIndex(FIELD.QTY_IN_SET)),
                        qtyInStore = cursor.getInt(cursor.getColumnIndex(FIELD.QTY_IN_STORE)),
                        colorId = cursor.getInt(cursor.getColumnIndex(FIELD.COLOR_ID)),
                        extra = cursor.getInt(cursor.getColumnIndex(FIELD.EXTRA))
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return parts
    }

    fun insertInventoryParts(parts: List<InventoryPart>) {
        val db = this.writableDatabase
        parts.forEach {
            val values = ContentValues()
            values.put(FIELD.INVENTORY_ID, it.inventoryId)
            values.put(FIELD.TYPE_ID, it.typeId)
            values.put(FIELD.ITEM_ID, it.itemId)
            values.put(FIELD.QTY_IN_SET, it.qtyInSet)
            values.put(FIELD.QTY_IN_STORE, it.qtyInStore)
            values.put(FIELD.COLOR_ID, it.colorId)
            values.put(FIELD.EXTRA, it.extra)
            db.insert(TABLE.INVENTORIES_PARTS, null, values) }
        db.close()
    }

    fun getTypeIdByCode(code: String) : Int {
        val query = "SELECT ${FIELD.ID} FROM ${TABLE.ITEM_TYPES} WHERE ${FIELD.CODE} LIKE \"$code\""
        val db = this.readableDatabase
        val cursor = db.rawQuery(query, null)
        cursor.moveToFirst()
        val id = cursor.getInt(cursor.getColumnIndex(FIELD.ID))
        cursor.close()
        db.close()
        return id
    }

    fun getTypeCodeById(id: Int) : String {
        val query = "SELECT ${FIELD.CODE} FROM ${TABLE.ITEM_TYPES} WHERE ${FIELD.ID} = $id"
        val db = this.readableDatabase
        val cursor = db.rawQuery(query, null)
        cursor.moveToFirst()
        val code = cursor.getString(cursor.getColumnIndex(FIELD.CODE))
        cursor.close()
        db.close()
        return code
    }

    fun getItemIdByCode(code: String) : Int {
        val query = "SELECT ${FIELD.ID} FROM ${TABLE.PARTS} WHERE ${FIELD.CODE} LIKE \"$code\""
        val db = this.readableDatabase
        val cursor = db.rawQuery(query, null)
        cursor.moveToFirst()
        val id = cursor.getInt(cursor.getColumnIndex(FIELD.ID))
        cursor.close()
        db.close()
        return id
    }

    fun getCodeByItemId(itemId: Int): String {
        val query = "SELECT ${FIELD.CODE} FROM ${TABLE.PARTS} WHERE ${FIELD.ID} = $itemId"
        val db = this.readableDatabase
        val cursor = db.rawQuery(query, null)
        cursor.moveToFirst()
        val code = cursor.getString(cursor.getColumnIndex(FIELD.CODE))
        cursor.close()
        db.close()
        return code
    }

    fun getNameByItemId(id: Int) : String {
        val query = "SELECT ${FIELD.NAME} FROM ${TABLE.PARTS} WHERE ${FIELD.ID} = $id"
        val db = this.readableDatabase
        val cursor = db.rawQuery(query, null)
        cursor.moveToFirst()
        val name = cursor.getString(cursor.getColumnIndex(FIELD.NAME))
        cursor.close()
        db.close()
        return name
    }

    fun getImage(itemId: Int, colorId: Int) : Bitmap? {
        val query = "SELECT ${FIELD.IMAGE} FROM ${TABLE.CODES} WHERE ${FIELD.ITEM_ID} = $itemId AND ${FIELD.COLOR_ID} = $colorId"
        val db = this.readableDatabase
        val cursor = db.rawQuery(query, null)
        var image : Bitmap? = null
        if(cursor.moveToFirst()) {
            val bytes = cursor.getBlob(cursor.getColumnIndex(FIELD.IMAGE))
            if (bytes != null){
                image = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
        }
        cursor.close()
        db.close()
        return image
    }

    fun insertImage(itemId: Int, colorId: Int, image: Bitmap) {
        val stream = ByteArrayOutputStream()
        image.compress(CompressFormat.PNG, 0, stream)
        val bytes = stream.toByteArray()
        val values = ContentValues()
        values.put(FIELD.IMAGE, bytes)
        val db = this.writableDatabase
        val rows = db.update(TABLE.CODES, values, " ${FIELD.ITEM_ID} = ? AND ${FIELD.COLOR_ID} = ?", arrayOf(itemId.toString(), colorId.toString()))
        if (rows == 0) {
            values.put(FIELD.ITEM_ID, itemId)
            values.put(FIELD.COLOR_ID, colorId)
            db.insert(TABLE.CODES, null, values)
        }
        db.close()
    }

    fun getImageCode(itemId: Int, colorId: Int) : Int? {
        val query = "SELECT ${FIELD.CODE} FROM ${TABLE.CODES} WHERE ${FIELD.ITEM_ID} = $itemId AND ${FIELD.COLOR_ID} = $colorId"
        val db = this.readableDatabase
        val cursor = db.rawQuery(query, null)
        var code : Int? = null
        if(cursor.moveToFirst()) {
           code = cursor.getInt(cursor.getColumnIndex(FIELD.CODE))
        }
        cursor.close()
        db.close()
        return code
    }

    fun increaseQty(invId: Int, itemId: Int) {
        val query = "UPDATE ${TABLE.INVENTORIES_PARTS} SET ${FIELD.QTY_IN_STORE} = ${FIELD.QTY_IN_STORE} + 1 WHERE ${FIELD.INVENTORY_ID} = $invId AND ${FIELD.ITEM_ID} = $itemId"
        val db = this.readableDatabase
        db.execSQL(query)
        db.close()
    }

    fun decreaseQty(invId: Int, itemId: Int) {
        val query = "UPDATE ${TABLE.INVENTORIES_PARTS} SET ${FIELD.QTY_IN_STORE} = ${FIELD.QTY_IN_STORE} - 1 WHERE ${FIELD.INVENTORY_ID} = $invId AND ${FIELD.ITEM_ID} = $itemId"
        val db = this.readableDatabase
        db.execSQL(query)
        db.close()
    }
}