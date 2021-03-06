// (c) Copyright 2015 Cloudera, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.cloudera.director.aws;

import static com.cloudera.director.aws.AWSClientConfig.AWSClientConfigurationPropertyToken;
import static com.cloudera.director.aws.ec2.DeviceMappingsConfigProperties.DeviceMappingsConfigurationPropertyToken;
import static com.cloudera.director.aws.ec2.EphemeralDeviceMappings.EphemeralDeviceMappingsConfigProperties.EphemeralDeviceMappingsConfigurationPropertyToken;
import static com.cloudera.director.aws.ec2.VirtualizationMappings.VirtualizationMappingsConfigProperties.VirtualizationMappingsConfigurationPropertyToken;
import static com.cloudera.director.aws.rds.RDSEndpoints.RDSEndpointsConfigProperties.RDSEndpointsConfigurationPropertyToken;

import com.cloudera.director.aws.common.ConfigFragmentWrapper;
import com.cloudera.director.aws.common.ResourceBundleLocalizationContext;
import com.cloudera.director.aws.ec2.EphemeralDeviceMappings;
import com.cloudera.director.aws.ec2.VirtualizationMappings;
import com.cloudera.director.aws.ec2.ebs.EBSDeviceMappings;
import com.cloudera.director.aws.ec2.ebs.EBSMetadata;
import com.cloudera.director.aws.network.NetworkRules;
import com.cloudera.director.aws.provider.AWSProvider;
import com.cloudera.director.aws.rds.RDSEncryptionInstanceClasses;
import com.cloudera.director.aws.rds.RDSEndpoints;
import com.cloudera.director.spi.v2.common.http.HttpProxyParameters;
import com.cloudera.director.spi.v2.model.ConfigurationPropertyToken;
import com.cloudera.director.spi.v2.model.Configured;
import com.cloudera.director.spi.v2.model.LocalizationContext;
import com.cloudera.director.spi.v2.model.util.SimpleConfiguration;
import com.cloudera.director.spi.v2.provider.CloudProvider;
import com.cloudera.director.spi.v2.provider.CloudProviderMetadata;
import com.cloudera.director.spi.v2.provider.util.AbstractLauncher;
import com.cloudera.director.spi.v2.util.ConfigurationPropertiesUtil;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ObjectArrays;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigParseOptions;
import com.typesafe.config.ConfigSyntax;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * AWS plugin launcher.
 */
@SuppressWarnings("PMD.TooManyStaticImports")
public class AWSLauncher extends AbstractLauncher {

  /**
   * The cloud provider metadata.
   */
  private static final List<CloudProviderMetadata> CLOUD_PROVIDER_METADATA =
      Collections.unmodifiableList(Collections.singletonList(AWSProvider.METADATA));

  /**
   * The default setting for using tag on create.
   */
  public static final boolean DEFAULT_USE_TAG_ON_CREATE = true;

  /**
   * Creates ephemeral device mappings with the specified parameters.
   *
   * @param config                   the configuration
   * @param configurationDirectory   the plugin configuration directory
   * @param cloudLocalizationContext the parent cloud localization context
   * @return the ephemeral device mappings
   */
  private static EphemeralDeviceMappings getEphemeralDeviceMappings(Config config,
      File configurationDirectory, LocalizationContext cloudLocalizationContext) {
    return new EphemeralDeviceMappings(getConfiguration(config,
        Configurations.EPHEMERAL_DEVICE_MAPPINGS_SECTION,
        getEphemeralConfigurationPropertyTokens()),
        configurationDirectory, cloudLocalizationContext);
  }

  /**
   * Creates EBS device mappings with the specified parameters.
   *
   * @param config                   the configuration
   * @param cloudLocalizationContext the parent cloud localization context
   * @return the ephemeral device mappings
   */
  private static EBSDeviceMappings getEbsDeviceMappings(Config config, LocalizationContext cloudLocalizationContext) {
    return new EBSDeviceMappings(getConfiguration(config,
        Configurations.EBS_DEVICE_MAPPINGS_SECTION,
        DeviceMappingsConfigurationPropertyToken.values()),
        cloudLocalizationContext);
  }

  private static ConfigurationPropertyToken[] getEphemeralConfigurationPropertyTokens() {
    ConfigurationPropertyToken[] ephemeralTokens = EphemeralDeviceMappingsConfigurationPropertyToken.values();
    ConfigurationPropertyToken[] deviceMappingTokens = DeviceMappingsConfigurationPropertyToken.values();
    return ObjectArrays.concat(ephemeralTokens, deviceMappingTokens, ConfigurationPropertyToken.class);
  }

  /**
   * Creates EBS metadata with the specified parameters.
   *
   * @param config                   the configuration
   * @param configurationDirectory   the plugin configuration directory
   * @param cloudLocalizationContext the parent cloud localization context
   * @return the EBS metadata
   */
  private static EBSMetadata getEBSMetadata(Config config,
      File configurationDirectory, LocalizationContext cloudLocalizationContext) {
    return new EBSMetadata(getConfiguration(config,
        Configurations.EBS_METADATA_SECTION,
        EBSMetadata.EBSMetadataConfigProperties.EBSMetadataConfigurationPropertyToken.values()),
        configurationDirectory, cloudLocalizationContext);
  }

  /**
   * Creates virtualization mappings with the specified parameters.
   *
   * @param config                   the configuration
   * @param configurationDirectory   the plugin configuration directory
   * @param cloudLocalizationContext the parent cloud localization context
   * @return the virtualization mappings
   */
  private static VirtualizationMappings getVirtualizationMappings(Config config,
      File configurationDirectory, LocalizationContext cloudLocalizationContext) {
    return new VirtualizationMappings(getConfiguration(config,
        Configurations.VIRTUALIZATION_MAPPINGS_SECTION,
        VirtualizationMappingsConfigurationPropertyToken.values()),
        configurationDirectory, cloudLocalizationContext);
  }

  /**
   * Creates RDS endpoints with the specified parameters.
   *
   * @param config                   the configuration
   * @param configurationDirectory   the plugin configuration directory
   * @param cloudLocalizationContext the parent cloud localization context
   * @return the RDS endpoints
   */
  private static RDSEndpoints getRDSEndpoints(Config config,
      File configurationDirectory, LocalizationContext cloudLocalizationContext) {
    return new RDSEndpoints(getConfiguration(config,
        Configurations.RDS_ENDPOINTS_SECTION,
        RDSEndpointsConfigurationPropertyToken.values()),
        configurationDirectory, cloudLocalizationContext);
  }

  /**
   * Creates RDS encryption instance classes with the specified parameters.
   *
   * @param config                   the configuration
   * @param configurationDirectory   the plugin configuration directory
   * @param cloudLocalizationContext the parent cloud localization context
   * @return the RDS encryption instance classes
   */
  private static RDSEncryptionInstanceClasses getRDSEncryptionInstanceClasses(Config config,
      File configurationDirectory, LocalizationContext cloudLocalizationContext) {
    return new RDSEncryptionInstanceClasses(getConfiguration(config,
        Configurations.RDS_ENCRYPTION_INSTANCE_CLASSES_SECTION,
        RDSEncryptionInstanceClasses.ConfigurationPropertyToken.values()),
        configurationDirectory, cloudLocalizationContext);
  }

  /**
   * Creates AWS client config with the specified parameters.
   *
   * @param config                   the configuration
   * @param httpProxyParameters      the HTTP proxy parameters
   * @param cloudLocalizationContext the parent cloud localization context
   * @return the AWS client config
   */
  private static AWSClientConfig getAWSClientConfig(Config config,
      HttpProxyParameters httpProxyParameters,
      LocalizationContext cloudLocalizationContext) {
    return new AWSClientConfig(getConfiguration(config, Configurations.AWS_CLIENT_SECTION,
        AWSClientConfigurationPropertyToken.values()), httpProxyParameters,
        cloudLocalizationContext);
  }

  /**
   * Creates AWS filter config with the specified parameters.
   *
   * @param config the configuration
   * @return the AWS filter config
   */
  @SuppressWarnings("PMD.UselessParentheses")
  private static AWSFilters getAWSFilterConfig(Config config) {
    return AWSFilters.getAWSFilters(
        ((config == null)
            || !config.hasPath(Configurations.AWS_FILTERS_SECTION))
            ? null
            : config.getConfig(Configurations.AWS_FILTERS_SECTION));
  }

  private static AWSTimeouts getAWSTimeouts(Config config) {
    return new AWSTimeouts(config == null || !config.hasPath(Configurations.AWS_TIMEOUTS_SECTION) ?
        null : config.getConfig(Configurations.AWS_TIMEOUTS_SECTION));
  }

  private static CustomTagMappings getCustomTagMappings(Config config) {
    return new CustomTagMappings(config == null || !config.hasPath(Configurations.CUSTOM_TAG_MAPPINGS_SECTION) ?
        null : config.getConfig(Configurations.CUSTOM_TAG_MAPPINGS_SECTION));
  }

  private static STSRoles getRoleConfigurations(Config config) {
    return config != null && config.hasPath(Configurations.STS_ROLES_SECTION)
        ? new STSRoles(config.getConfigList(Configurations.STS_ROLES_SECTION))
        : STSRoles.DEFAULT;
  }

  private static boolean getUseTagOnCreate(Config config) {
    return config != null && config.hasPath(Configurations.USE_TAG_ON_CREATE) ?
        config.getBoolean(Configurations.USE_TAG_ON_CREATE) : DEFAULT_USE_TAG_ON_CREATE;
  }

  private static <T extends ConfigurationPropertyToken> Configured getConfiguration(Config config,
      String section, T[] configurationPropertyTokens) {
    if (config == null) {
      return new SimpleConfiguration();
    }
    if (!config.hasPath(section)) {
      return new SimpleConfiguration();
    }
    Config sectionConfig = config.getConfig(section);
    //noinspection unchecked
    return new ConfigFragmentWrapper(sectionConfig,
        ConfigurationPropertiesUtil.asConfigurationPropertyList(configurationPropertyTokens));
  }

  @VisibleForTesting
  EphemeralDeviceMappings ephemeralDeviceMappings;

  @VisibleForTesting
  EBSDeviceMappings ebsDeviceMappings;

  @VisibleForTesting
  EBSMetadata ebsMetadata;

  @VisibleForTesting
  VirtualizationMappings virtualizationMappings;

  @VisibleForTesting
  RDSEndpoints rdsEndpoints;

  @VisibleForTesting
  RDSEncryptionInstanceClasses rdsEncryptionInstanceClasses;

  @VisibleForTesting
  AWSClientConfig awsClientConfig;

  @VisibleForTesting
  AWSFilters awsFilters;

  @VisibleForTesting
  AWSTimeouts awsTimeouts;

  @VisibleForTesting
  CustomTagMappings customTagMappings;

  private NetworkRules networkRules;

  @VisibleForTesting
  STSRoles stsRoles;

  @VisibleForTesting
  protected boolean useTagOnCreate;

  /**
   * Creates an AWS launcher.
   */
  public AWSLauncher() {
    super(CLOUD_PROVIDER_METADATA, ResourceBundleLocalizationContext.FACTORY);
  }

  @Override
  public void initialize(File configurationDirectory, HttpProxyParameters httpProxyParameters) {
    File configFile = new File(configurationDirectory, Configurations.CONFIGURATION_FILE_NAME);
    Config config = null;
    if (configFile.canRead()) {
      config = parseConfigFile(configFile);
    }

    // Configure HTTP proxy settings
    LocalizationContext rootLocalizationContext = getLocalizationContext(Locale.getDefault());

    // Configure AWS settings
    // Although these objects are configured at the launcher level,
    // we want their localization contexts to be nested inside the cloud provider context.
    LocalizationContext cloudLocalizationContext =
        AWSProvider.METADATA.getLocalizationContext(rootLocalizationContext);
    ebsDeviceMappings = getEbsDeviceMappings(config, cloudLocalizationContext);
    ephemeralDeviceMappings = getEphemeralDeviceMappings(config, configurationDirectory,
        cloudLocalizationContext);
    ebsMetadata = getEBSMetadata(config, configurationDirectory, cloudLocalizationContext);
    virtualizationMappings = getVirtualizationMappings(config, configurationDirectory,
        cloudLocalizationContext);
    rdsEndpoints = getRDSEndpoints(config, configurationDirectory, cloudLocalizationContext);
    rdsEncryptionInstanceClasses = getRDSEncryptionInstanceClasses(config, configurationDirectory,
        cloudLocalizationContext);
    awsClientConfig = getAWSClientConfig(config,
        (httpProxyParameters == null) ? new HttpProxyParameters() : httpProxyParameters,
        cloudLocalizationContext);
    awsFilters = getAWSFilterConfig(config);
    awsTimeouts = getAWSTimeouts(config);
    customTagMappings = getCustomTagMappings(config);
    networkRules = getNetworkRules(configurationDirectory);
    stsRoles = getRoleConfigurations(config);
    useTagOnCreate = getUseTagOnCreate(config);
  }

  /**
   * Retrieves the network rules from its configuration file.
   *
   * @param configurationDirectory the directory holding the network rules configuration file
   * @return the network rules
   */
  private NetworkRules getNetworkRules(File configurationDirectory) {
    File configFile = new File(configurationDirectory, Configurations.NETWORK_RULES_FILE_NAME);
    Config config = null;
    if (configFile.canRead()) {
      config = parseConfigFile(configFile);
    }
    return NetworkRules.fromConfig(config);
  }

  @Override
  public CloudProvider createCloudProvider(String cloudProviderId, Configured configuration,
      Locale locale) {
    if (!AWSProvider.METADATA.getId().equals(cloudProviderId)) {
      throw new IllegalArgumentException("Invalid cloud provider: " + cloudProviderId);
    }
    return new AWSProvider(configuration, ephemeralDeviceMappings, ebsDeviceMappings, ebsMetadata,
        virtualizationMappings, rdsEndpoints, rdsEncryptionInstanceClasses, awsClientConfig,
        awsFilters, awsTimeouts, customTagMappings, networkRules, stsRoles, useTagOnCreate,
        getLocalizationContext(locale));
  }

  /**
   * Parses the specified configuration file.
   *
   * @param configFile the configuration file
   * @return the parsed configuration
   */
  private static Config parseConfigFile(File configFile) {
    ConfigParseOptions options = ConfigParseOptions.defaults()
        .setSyntax(ConfigSyntax.CONF)
        .setAllowMissing(false);

    return ConfigFactory.parseFileAnySyntax(configFile, options);
  }
}
