/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.objectstorage;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.Principals;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.annotation.ServiceOperation;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.PropertyDirectory;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.context.NoSuchContextException;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.objectstorage.bittorrent.Tracker;
import com.eucalyptus.objectstorage.entities.BucketInfo;
import com.eucalyptus.objectstorage.entities.ObjectStorageGatewayInfo;
import com.eucalyptus.objectstorage.exceptions.AccessDeniedException;
import com.eucalyptus.objectstorage.exceptions.NotImplementedException;
import com.eucalyptus.objectstorage.msgs.AddObjectResponseType;
import com.eucalyptus.objectstorage.msgs.AddObjectType;
import com.eucalyptus.objectstorage.msgs.CopyObjectResponseType;
import com.eucalyptus.objectstorage.msgs.CopyObjectType;
import com.eucalyptus.objectstorage.msgs.CreateBucketResponseType;
import com.eucalyptus.objectstorage.msgs.CreateBucketType;
import com.eucalyptus.objectstorage.msgs.DeleteBucketResponseType;
import com.eucalyptus.objectstorage.msgs.DeleteBucketType;
import com.eucalyptus.objectstorage.msgs.DeleteObjectResponseType;
import com.eucalyptus.objectstorage.msgs.DeleteObjectType;
import com.eucalyptus.objectstorage.msgs.DeleteVersionResponseType;
import com.eucalyptus.objectstorage.msgs.DeleteVersionType;
import com.eucalyptus.objectstorage.msgs.GetBucketAccessControlPolicyResponseType;
import com.eucalyptus.objectstorage.msgs.GetBucketAccessControlPolicyType;
import com.eucalyptus.objectstorage.msgs.GetBucketLocationResponseType;
import com.eucalyptus.objectstorage.msgs.GetBucketLocationType;
import com.eucalyptus.objectstorage.msgs.GetBucketLoggingStatusResponseType;
import com.eucalyptus.objectstorage.msgs.GetBucketLoggingStatusType;
import com.eucalyptus.objectstorage.msgs.GetBucketVersioningStatusResponseType;
import com.eucalyptus.objectstorage.msgs.GetBucketVersioningStatusType;
import com.eucalyptus.objectstorage.msgs.GetObjectAccessControlPolicyResponseType;
import com.eucalyptus.objectstorage.msgs.GetObjectAccessControlPolicyType;
import com.eucalyptus.objectstorage.msgs.GetObjectExtendedResponseType;
import com.eucalyptus.objectstorage.msgs.GetObjectExtendedType;
import com.eucalyptus.objectstorage.msgs.GetObjectResponseType;
import com.eucalyptus.objectstorage.msgs.GetObjectStorageConfigurationResponseType;
import com.eucalyptus.objectstorage.msgs.GetObjectStorageConfigurationType;
import com.eucalyptus.objectstorage.msgs.GetObjectType;
import com.eucalyptus.objectstorage.msgs.HeadBucketResponseType;
import com.eucalyptus.objectstorage.msgs.HeadBucketType;
import com.eucalyptus.objectstorage.msgs.ListAllMyBucketsResponseType;
import com.eucalyptus.objectstorage.msgs.ListAllMyBucketsType;
import com.eucalyptus.objectstorage.msgs.ListBucketResponseType;
import com.eucalyptus.objectstorage.msgs.ListBucketType;
import com.eucalyptus.objectstorage.msgs.ListVersionsResponseType;
import com.eucalyptus.objectstorage.msgs.ListVersionsType;
import com.eucalyptus.objectstorage.msgs.ObjectStorageRequestType;
import com.eucalyptus.objectstorage.msgs.PostObjectResponseType;
import com.eucalyptus.objectstorage.msgs.PostObjectType;
import com.eucalyptus.objectstorage.msgs.PutObjectInlineResponseType;
import com.eucalyptus.objectstorage.msgs.PutObjectInlineType;
import com.eucalyptus.objectstorage.msgs.PutObjectType;
import com.eucalyptus.objectstorage.msgs.SetBucketAccessControlPolicyResponseType;
import com.eucalyptus.objectstorage.msgs.SetBucketAccessControlPolicyType;
import com.eucalyptus.objectstorage.msgs.SetBucketLoggingStatusResponseType;
import com.eucalyptus.objectstorage.msgs.SetBucketLoggingStatusType;
import com.eucalyptus.objectstorage.msgs.SetBucketVersioningStatusResponseType;
import com.eucalyptus.objectstorage.msgs.SetBucketVersioningStatusType;
import com.eucalyptus.objectstorage.msgs.SetObjectAccessControlPolicyResponseType;
import com.eucalyptus.objectstorage.msgs.SetObjectAccessControlPolicyType;
import com.eucalyptus.objectstorage.msgs.SetRESTBucketAccessControlPolicyResponseType;
import com.eucalyptus.objectstorage.msgs.SetRESTBucketAccessControlPolicyType;
import com.eucalyptus.objectstorage.msgs.SetRESTObjectAccessControlPolicyResponseType;
import com.eucalyptus.objectstorage.msgs.SetRESTObjectAccessControlPolicyType;
import com.eucalyptus.objectstorage.msgs.UpdateObjectStorageConfigurationResponseType;
import com.eucalyptus.objectstorage.msgs.UpdateObjectStorageConfigurationType;
import com.eucalyptus.objectstorage.policy.RequiresPermission;
import com.eucalyptus.objectstorage.policy.ResourceType;
import com.eucalyptus.objectstorage.util.ObjectStorageProperties;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Lookups;
import com.google.common.base.Function;
import com.google.common.base.Strings;

import edu.ucsb.eucalyptus.msgs.BaseDataChunk;
import edu.ucsb.eucalyptus.msgs.ComponentProperty;
import edu.ucsb.eucalyptus.util.SystemUtil;

/**
 * Operation handler for the ObjectStorageGateway. Main point of entry
 * This class handles user and system requests.
 *
 */
public class ObjectStorageGateway {
	private static Logger LOG = Logger.getLogger( ObjectStorageGateway.class );

	private static ObjectStorageProviderClient ospClient = null;
	protected static ConcurrentHashMap<String, ChannelBuffer> streamDataMap = new ConcurrentHashMap<String, ChannelBuffer>();

	public ObjectStorageProviderClient getClient() {
		return ospClient;
	}

	public static void checkPreconditions() throws EucalyptusCloudException, ExecutionException {}

	public static void configure() {		
		synchronized(ObjectStorageGateway.class) {
			if(ospClient == null) {		
				ObjectStorageGatewayInfo osgInfo = ObjectStorageGatewayInfo.getObjectStorageGatewayInfo();
				try {
					ospClient = ObjectStorageProviders.getInstance();
				} catch (Exception ex) {
					LOG.error (ex);
				}
			}
		}

		String limits = System.getProperty(ObjectStorageProperties.USAGE_LIMITS_PROPERTY);
		if(limits != null) {
			ObjectStorageProperties.shouldEnforceUsageLimits = Boolean.parseBoolean(limits);
		}
		try {
			ospClient.check();
		} catch(EucalyptusCloudException ex) {
			LOG.error("Error initializing walrus", ex);
			SystemUtil.shutdownWithError(ex.getMessage());
		}

		//Disable torrents
		//Tracker.initialize();
		if(System.getProperty("euca.virtualhosting.disable") != null) {
			ObjectStorageProperties.enableVirtualHosting = false;
		}
		try {
			if (ospClient != null) {
				ospClient.start();
			}
		} catch(EucalyptusCloudException ex) {
			LOG.error("Error starting storage backend: " + ex);			
		}		
	}

	public ObjectStorageGateway() {}

	public static void enable() throws EucalyptusCloudException {
		ospClient.enable();
	}

	public static void disable() throws EucalyptusCloudException {		
		ospClient.disable();

		//flush the data stream buffer, disconnect clients.
		streamDataMap.clear();
	}

	public static void check() throws EucalyptusCloudException {
		ospClient.check();
	}

	public static void stop() throws EucalyptusCloudException {
		ospClient.stop();
		Tracker.die();
		ObjectStorageProperties.shouldEnforceUsageLimits = true;
		ObjectStorageProperties.enableVirtualHosting = true;

		//Be sure it's empty
		streamDataMap.clear();
	}

	public UpdateObjectStorageConfigurationResponseType UpdateObjectStorageConfiguration(UpdateObjectStorageConfigurationType request) throws EucalyptusCloudException {
		UpdateObjectStorageConfigurationResponseType reply = (UpdateObjectStorageConfigurationResponseType) request.getReply();
		if(ComponentIds.lookup(Eucalyptus.class).name( ).equals(request.getEffectiveUserId()))
			throw new AccessDeniedException("Only admin can change walrus properties.");
		if(request.getProperties() != null) {
			for(ComponentProperty prop : request.getProperties()) {
				LOG.info("ObjectStorage property: " + prop.getDisplayName() + " Qname: " + prop.getQualifiedName() + " Value: " + prop.getValue());
				try {
					ConfigurableProperty entry = PropertyDirectory.getPropertyEntry(prop.getQualifiedName());
					//type parser will correctly covert the value
					entry.setValue(prop.getValue());
				} catch (IllegalAccessException e) {
					LOG.error(e, e);
				}
			}
		}
		String name = request.getName();
		ospClient.check();
		return reply;
	}

	public GetObjectStorageConfigurationResponseType GetObjectStorageConfiguration(GetObjectStorageConfigurationType request) throws EucalyptusCloudException {
		GetObjectStorageConfigurationResponseType reply = (GetObjectStorageConfigurationResponseType) request.getReply();
		ConfigurableClass configurableClass = ObjectStorageGatewayInfo.class.getAnnotation(ConfigurableClass.class);
		if(configurableClass != null) {
			String prefix = configurableClass.root();
			reply.setProperties((ArrayList<ComponentProperty>) PropertyDirectory.getComponentPropertySet(prefix));
		}
		return reply;
	}

	@ServiceOperation
	public enum HandleFirstChunk implements Function<PutObjectType, Object> {
		INSTANCE;

		@Override
		public Object apply(PutObjectType request) {
			LOG.debug("Processing PutObject request (direct dispatch) " + request.toString() + " id: " + request.getCorrelationId());
			ChannelBuffer b =  ChannelBuffers.dynamicBuffer();
			if(!streamDataMap.containsKey(request.getCorrelationId())) {
				byte[] firstChunk = request.getData();
				b.writeBytes(firstChunk);
				streamDataMap.put(request.getCorrelationId(), b);
			} else {
				//This should not happen. CorrelationIds should be unique for each request
				LOG.error("CorrelationId lookup in data map found duplicate. Unexpected error");
				return null;
			}
			try {
				Object o = ospClient.putObject(request, new ChannelBufferStreamingInputStream(b));
				return o;
			} catch (EucalyptusCloudException ex) {
				LOG.error(ex);
				return null;
			}
		}

	}

	@ServiceOperation
	public enum HandleChunk implements Function<BaseDataChunk, Object> {
		INSTANCE;
		private static final int retryCount = 15;
		@Override
		public Object apply(BaseDataChunk chunk) {
			/*
			 * This works because chunks are delivered in-order through netty.
			 */
			String id = chunk.getCorrelationId();
			LOG.debug("Processing data chunk with id: " + id + " Last? " + chunk.isLast());
			try {
				ChannelBuffer writeBuffer = streamDataMap.get(id);
				//TODO: HACK! This should be handoff through a monitor.
				//This is required because there is a race between the first chunk
				//and the first data-only chunk.
				//Hacking around it to make progress on other ops, should revisit -ns
				for (int i=0; i < retryCount; ++i) {
					if (writeBuffer == null) {
						LOG.info("Stream Data Map is empty, retrying: " + (i + 1) + " of " + retryCount);
						Thread.sleep(100);
						writeBuffer = streamDataMap.get(id);
					}
				}
				writeBuffer.writeBytes(chunk.getContent());
			} catch (Exception e) {
				LOG.error(e, e);
			}
			return null;
		}	
	}


	/**
	 * Does the current request have an authenticated user? Or is it anonymous?
	 * @return
	 */
	protected static boolean isUserAnonymous() {
		Context ctx = Contexts.lookup();
		return (ctx.getAccount() == null || ctx.getAccount().equals(Principals.nobodyAccount()));
	}

	/**
	 * Does the current context have admin priviledges
	 * @return
	 */
	protected static boolean isAdminUser() {
		return Contexts.lookup().hasAdministrativePrivileges();
	}

	/**
	 * Evaluates the IAM policy for the operation requested
	 * @param request
	 * @param resourceId optional (can be null) explicit resourceId to check. If null, the request is used to get the resource.
	 * @return
	 */
	protected static <T extends ObjectStorageRequestType> boolean operationAllowed(T request, final String optionalResourceId, final String optionalOwnerId) throws IllegalArgumentException {
		if(isAdminUser()) {
			//System admin can do anything
			return true;
		}

		String resourceOwnerAccountId = optionalOwnerId;
		if(Strings.isNullOrEmpty(resourceOwnerAccountId)) {
			try {
				Account userAccount = null;
				try {
					userAccount = Contexts.lookup(request.getCorrelationId()).getAccount();
				} catch(NoSuchContextException e) {
					//This is not an expected path, but if no context found use the request credentials itself
					if(!Strings.isNullOrEmpty(request.getEffectiveUserId())) {
						userAccount = Accounts.lookupAccessKeyById(request.getEffectiveUserId()).getUser().getAccount();
					} else if(!Strings.isNullOrEmpty(request.getAccessKeyID())) {
						userAccount = Accounts.lookupAccessKeyById(request.getAccessKeyID()).getUser().getAccount();
					}
				}
				if(userAccount != null) {
					resourceOwnerAccountId = userAccount.getAccountNumber();
				} else {
					throw new AuthException("Could not find resource owner account ID");
				}
			} catch (AuthException e) {
				LOG.error("Failed to get owner account id for request, cannot verify authorization: " + e.getMessage(), e);				
				return false;
			}
		}
		
		String[] actions = request.getClass().getAnnotation(RequiresPermission.class).value();
		if(actions == null) {
			throw new IllegalArgumentException("No requires-permission actions found in request class annotations, cannot process.");
		}

		String resourceType = request.getClass().getAnnotation(ResourceType.class).value();
		if(resourceType == null) {
			throw new IllegalArgumentException("No resource type found in request class annotations, cannot process.");
		}		

		String resourceId = optionalResourceId;		
		if(resourceId == null ) {
			if(resourceType.equals(PolicySpec.S3_RESOURCE_BUCKET)) {
				resourceId = request.getBucket();
			} else if(resourceType.equals(PolicySpec.S3_RESOURCE_OBJECT)) {
				resourceId = request.getKey();
			}
		}

		for(String action : actions ) {
			if(!Lookups.checkPrivilege(action, PolicySpec.VENDOR_S3, resourceType, resourceId, resourceOwnerAccountId)) {
				return false;
			}			
		}

		return true;
	}

	public HeadBucketResponseType HeadBucket(HeadBucketType request) throws EucalyptusCloudException {
		LOG.info("Handling HeadBucket request");
		return ospClient.headBucket(request);
	}

	public CreateBucketResponseType CreateBucket(CreateBucketType request) throws EucalyptusCloudException {
		LOG.info("Handling CreateBucket request");		
		return ospClient.createBucket(request);
	}

	public DeleteBucketResponseType DeleteBucket(DeleteBucketType request) throws EucalyptusCloudException {
		LOG.info("Handling DeleteBucket request");
		return ospClient.deleteBucket(request);
	}

	public ListAllMyBucketsResponseType ListAllMyBuckets(ListAllMyBucketsType request) throws EucalyptusCloudException {
		LOG.info("Handling ListAllBuckets request");
		Context ctx = Contexts.lookup();
		Account account = ctx.getAccount();
		if (account == null) {
			throw new AccessDeniedException("no such account");
		}				
		ListAllMyBucketsResponseType response = ospClient.listAllMyBuckets(request);
		return response;
	}

	public GetBucketAccessControlPolicyResponseType GetBucketAccessControlPolicy(GetBucketAccessControlPolicyType request) throws EucalyptusCloudException
	{
		return ospClient.getBucketAccessControlPolicy(request);
	}

	public PostObjectResponseType PostObject (PostObjectType request) throws EucalyptusCloudException {
		return ospClient.postObject(request);
	}

	public PutObjectInlineResponseType PutObjectInline (PutObjectInlineType request) throws EucalyptusCloudException {
		return ospClient.putObjectInline(request);
	}

	public AddObjectResponseType AddObject (AddObjectType request) throws EucalyptusCloudException {
		return ospClient.addObject(request);
	}

	public DeleteObjectResponseType DeleteObject (DeleteObjectType request) throws EucalyptusCloudException {
		return ospClient.deleteObject(request);
	}

	public ListBucketResponseType ListBucket(ListBucketType request) throws EucalyptusCloudException {
		return ospClient.listBucket(request);
	}

	public GetObjectAccessControlPolicyResponseType GetObjectAccessControlPolicy(GetObjectAccessControlPolicyType request) throws EucalyptusCloudException
	{
		return ospClient.getObjectAccessControlPolicy(request);
	}

	public SetBucketAccessControlPolicyResponseType SetBucketAccessControlPolicy(SetBucketAccessControlPolicyType request) throws EucalyptusCloudException
	{
		return ospClient.setBucketAccessControlPolicy(request);
	}

	public SetObjectAccessControlPolicyResponseType SetObjectAccessControlPolicy(SetObjectAccessControlPolicyType request) throws EucalyptusCloudException
	{
		return ospClient.setObjectAccessControlPolicy(request);
	}

	public SetRESTBucketAccessControlPolicyResponseType SetRESTBucketAccessControlPolicy(SetRESTBucketAccessControlPolicyType request) throws EucalyptusCloudException
	{
		return ospClient.setRESTBucketAccessControlPolicy(request);
	}

	public SetRESTObjectAccessControlPolicyResponseType SetRESTObjectAccessControlPolicy(SetRESTObjectAccessControlPolicyType request) throws EucalyptusCloudException
	{
		return ospClient.setRESTObjectAccessControlPolicy(request);
	}

	public GetObjectResponseType GetObject(GetObjectType request) throws EucalyptusCloudException {
		ospClient.getObject(request);
		//ObjectGetter getter = new ObjectGetter(request);
		//Threads.lookup(ObjectStorage.class, ObjectStorageGateway.ObjectGetter.class).limitTo(1).submit(getter);
		return null;
	}

	private class ObjectGetter implements Runnable {
		GetObjectType request;
		public ObjectGetter(GetObjectType request) {
			this.request = request;
		}
		@Override
		public void run() {
			try {
				ospClient.getObject(request);
			} catch (Exception ex) {
				LOG.error(ex, ex);
			}
		}

	}
	public GetObjectExtendedResponseType GetObjectExtended(GetObjectExtendedType request) throws EucalyptusCloudException {
		return ospClient.getObjectExtended(request);
	}

	public GetBucketLocationResponseType GetBucketLocation(GetBucketLocationType request) throws EucalyptusCloudException {
		return ospClient.getBucketLocation(request);
	}

	public CopyObjectResponseType CopyObject(CopyObjectType request) throws EucalyptusCloudException {
		return ospClient.copyObject(request);
	}

	public GetBucketLoggingStatusResponseType GetBucketLoggingStatus(GetBucketLoggingStatusType request) throws EucalyptusCloudException {
		return ospClient.getBucketLoggingStatus(request);
	}

	public SetBucketLoggingStatusResponseType SetBucketLoggingStatus(SetBucketLoggingStatusType request) throws EucalyptusCloudException {
		return ospClient.setBucketLoggingStatus(request);
	}

	public GetBucketVersioningStatusResponseType GetBucketVersioningStatus(GetBucketVersioningStatusType request) throws EucalyptusCloudException {
		return ospClient.getBucketVersioningStatus(request);
	}

	public SetBucketVersioningStatusResponseType SetBucketVersioningStatus(SetBucketVersioningStatusType request) throws EucalyptusCloudException {
		return ospClient.setBucketVersioningStatus(request);
	}

	public ListVersionsResponseType ListVersions(ListVersionsType request) throws EucalyptusCloudException {
		return ospClient.listVersions(request);
	}

	public DeleteVersionResponseType DeleteVersion(DeleteVersionType request) throws EucalyptusCloudException {
		return ospClient.deleteVersion(request);
	}

	public static InetAddress getBucketIp(String bucket) throws EucalyptusCloudException {
		EntityWrapper<BucketInfo> db = EntityWrapper.get(BucketInfo.class);
		try {
			BucketInfo searchBucket = new BucketInfo(bucket);
			db.getUniqueEscape(searchBucket);
			return ObjectStorageProperties.getWalrusAddress();
		} catch (EucalyptusCloudException ex) {
			throw ex;
		} finally {
			db.rollback();
		}
	}

}