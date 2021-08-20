package at.bitfire.davdroid.syncadapter.groups

import android.content.ContentUris
import android.provider.ContactsContract
import at.bitfire.davdroid.log.Logger
import at.bitfire.davdroid.resource.LocalAddress
import at.bitfire.davdroid.resource.LocalGroup
import at.bitfire.davdroid.syncadapter.ContactsSyncManager
import at.bitfire.davdroid.syncadapter.ContactsSyncManager.Companion.disjunct
import at.bitfire.vcard4android.BatchOperation
import at.bitfire.vcard4android.Contact
import java.io.FileNotFoundException

class VCard4Strategy(val syncManager: ContactsSyncManager): ContactGroupStrategy {

    private val localCollection = syncManager.localCollection

    override fun beforeUploadDirty() {
        /* Mark groups with changed members as dirty:
           1. Iterate over all dirty contacts.
           2. Check whether group memberships have changed by comparing group memberships and cached group memberships.
           3. Mark groups which have been added to/removed from the contact as dirty so that they will be uploaded.
           4. Successful upload will reset dirty flag and update cached group memberships.
         */
        val batch = BatchOperation(localCollection.provider!!)
        for (contact in localCollection.findDirtyContacts())
            try {
                Logger.log.fine("Looking for changed group memberships of contact ${contact.fileName}")
                val cachedGroups = contact.getCachedGroupMemberships()
                val currentGroups = contact.getGroupMemberships()
                for (groupID in cachedGroups disjunct currentGroups) {
                    Logger.log.fine("Marking group as dirty: $groupID")
                    batch.enqueue(BatchOperation.CpoBuilder
                            .newUpdate(localCollection.syncAdapterURI(ContentUris.withAppendedId(ContactsContract.Groups.CONTENT_URI, groupID)))
                            .withValue(ContactsContract.Groups.DIRTY, 1))
                }
            } catch(e: FileNotFoundException) {
            }
        batch.commit()
    }

    override fun verifyContactBeforeSaving(contact: Contact) {
    }

    override fun postProcess() {
        Logger.log.info("Assigning memberships of downloaded contact groups")
        LocalGroup.applyPendingMemberships(localCollection)
    }

}