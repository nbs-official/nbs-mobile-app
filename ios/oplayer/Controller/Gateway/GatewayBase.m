//
//  GatewayBase.m
//  oplayer
//
//  Created by SYALON on 13-11-20.
//
//

#import "GatewayBase.h"
#import "OrgUtils.h"
#import "VCBase.h"

@interface GatewayBase()
{
    NSDictionary*   _api_config_json;
}

@end

@implementation GatewayBase

@synthesize api_config_json = _api_config_json;

- (void)dealloc
{
    self.api_config_json = nil;
}

- (id)initWithApiConfig:(NSDictionary*)api_config_json
{
    assert(api_config_json);
    self = [super init];
    if (self)
    {
        assert([api_config_json objectForKey:@"base"]);
        self.api_config_json = api_config_json;
    }
    return self;
}

- (WsPromise*)queryCoinList
{
    id api_base = [self.api_config_json objectForKey:@"base"];
    id coin_list = self.api_config_json[@"coin_list"];
    assert(coin_list);
    
    id active_wallets = self.api_config_json[@"active_wallets"];
    assert(active_wallets);
    
    id trading_pairs = self.api_config_json[@"trading_pairs"];
    assert(trading_pairs);
    
    id coinlist_url =  [NSString stringWithFormat:@"%@%@", api_base, coin_list];
    id active_wallets_url =  [NSString stringWithFormat:@"%@%@", api_base, active_wallets];
    id trading_pairs_url =  [NSString stringWithFormat:@"%@%@", api_base, trading_pairs];
    id p1 = [OrgUtils asyncFetchUrl:coinlist_url args:nil];
    id p2 = [OrgUtils asyncFetchUrl:active_wallets_url args:nil];
    id p3 = [OrgUtils asyncFetchUrl:trading_pairs_url args:nil];
    return [WsPromise all:@[p1, p2, p3]];
}

- (NSArray*)processCoinListData:(NSArray*)data_array balanceHash:(NSDictionary*)balanceHash
{
    //  Openledger responsed data
    //{
    //    "allow_deposit" = 0;
    //    "allow_withdrawal" = 0;
    //    authorized = "<null>";
    //    backingCoinType = eosdac;
    //    coinPriora = 0;
    //    coinType = "open.eosdac";
    //    gateFee = "0.000000";
    //    intermediateAccount = "openledger-dex";
    //    "is_return_active" = 0;
    //    maintenanceReason = "Under maintenance";
    //    name = "OL EOSDAC";
    //    notAuthorizedReasons = "<null>";
    //    precision = "100000.00000000000000000000";
    //    restricted = 0;
    //    supportsOutputMemos = 1;
    //    symbol = "OPEN.EOSDAC";
    //    transactionFee = 0;
    //    walletName = "BitShares 2.0";
    //    walletSymbol = "OPEN.EOSDAC";
    //    walletType = bitshares2;
    //    withdrawalLimit24h = "-1";
    //},
    //{
    //    "allow_deposit" = 1;
    //    "allow_withdrawal" = 1;
    //    authorized = "<null>";
    //    backingCoinType = "<null>";
    //    coinPriora = 0;
    //    coinType = bitcny;
    //    gateFee = "40.000000";
    //    intermediateAccount = "openledger-dex";
    //    "is_return_active" = 1;
    //    maintenanceReason = "";
    //    name = BITCNY;
    //    notAuthorizedReasons = "<null>";
    //    precision = "10000.00000000000000000000";
    //    restricted = 0;
    //    supportsOutputMemos = 0;
    //    symbol = BITCNY;
    //    transactionFee = 0;
    //    walletName = "Ethereum BITCNY token";
    //    walletSymbol = BITCNY;
    //    walletType = bitcny;
    //    withdrawalLimit24h = "-1";
    //},
    
    //  GDEX responsed data
    //{
    //    authorized = "<null>";
    //    backingCoinType = "<null>";
    //    coinPriora = 1;
    //    coinType = btc;
    //    gateFee = "0.001";
    //    intermediateAccount = "gdex-wallet";
    //    maintenanceReason = "";
    //    maxAmount = 999999999;
    //    minAmount = "0.002";
    //    name = bitcoin;
    //    notAuthorizedReasons = "<null>";
    //    precision = 100000000;
    //    restricted = 0;
    //    supportsOutputMemos = 0;
    //    symbol = BTC;
    //    transactionFee = 0;
    //    walletName = BTC;
    //    walletSymbol = BTC;
    //    walletType = btc;
    //},
    //{
    //    authorized = "<null>";
    //    backingCoinType = btc;
    //    coinPriora = 1;
    //    coinType = "gdex.btc";
    //    gateFee = 0;
    //    intermediateAccount = "gdex-wallet";
    //    maintenanceReason = "";
    //    maxAmount = 999999999;
    //    minAmount = "0.00000001";
    //    name = bitcoin;
    //    notAuthorizedReasons = "<null>";
    //    precision = 100000000;
    //    restricted = 0;
    //    supportsOutputMemos = 0;
    //    symbol = "GDEX.BTC";
    //    transactionFee = 0;
    //    walletName = "bitshares2.0";
    //    walletSymbol = "GDEX.BTC";
    //    walletType = "bitshares2.0";
    //}
    
    id data_coinlist = [data_array objectAtIndex:0];
    id data_active_wallets = [data_array objectAtIndex:1];
    id data_trading_pairs = [data_array objectAtIndex:2];
    //  ??????????????????????????????????????????
    if (!data_coinlist || [data_coinlist isKindOfClass:[NSNull class]]){
        return nil;
    }
    if (!data_active_wallets || [data_active_wallets isKindOfClass:[NSNull class]]){
        return nil;
    }
    if (!data_trading_pairs || [data_trading_pairs isKindOfClass:[NSNull class]]){
        return nil;
    }
    //  ???3?????????????????????
    //  - ???????????????
    NSMutableDictionary* trading_hash = [NSMutableDictionary dictionary];
    for (id item in data_trading_pairs) {
        id inputCoinType = [item objectForKey:@"inputCoinType"];
        id outputCoinType = [item objectForKey:@"outputCoinType"];
        if (!inputCoinType || !outputCoinType){
            continue;
        }
        [trading_hash setObject:outputCoinType forKey:inputCoinType];
    }
    //  - ????????????
    NSMutableDictionary* wallettype_hash = [NSMutableDictionary dictionary];
    for (id walletType in data_active_wallets) {
        [wallettype_hash setObject:@YES forKey:walletType];
    }
    NSMutableDictionary* coin_hash = [NSMutableDictionary dictionary];
    NSMutableDictionary* coin_wallettype_hash = [NSMutableDictionary dictionary];
    for (id item in data_coinlist) {
        id coinType = [item objectForKey:@"coinType"];
        if (!coinType){
            continue;
        }
        [coin_hash setObject:item forKey:coinType];
        id walletType = [item objectForKey:@"walletType"];
        if (!walletType){
            continue;
        }
        [coin_wallettype_hash setObject:walletType forKey:coinType];
    }
    
    NSMutableArray* result = [NSMutableArray array];
    for (id item in data_coinlist) {
        id backingCoinType = [item objectForKey:@"backingCoinType"];
        if (backingCoinType && [backingCoinType isKindOfClass:[NSString class]]){
            //  ?????????????????????
            id backingCoinItem = [coin_hash objectForKey:backingCoinType];
            if (!backingCoinItem){
                continue;
            }
            
            id coinType = [item objectForKey:@"coinType"];
            
            //  ???????????????
            BOOL enableDeposit = [[trading_hash objectForKey:backingCoinType] isEqualToString:coinType];
            BOOL enableWithdraw = [[trading_hash objectForKey:coinType] isEqualToString:backingCoinType];
            
            //  TODO:1.6 openledger??? active_wallet ????????? bitshares2.0?????? ???????????????
            
            //  ?????????????????????walletType
            //            id coin_walletType = coin_wallettype_hash[coinType];
            id back_walletType = coin_wallettype_hash[backingCoinType];
            if (/*coin_walletType && */backingCoinType){
                //  ?????????????????????????????????????????????????????????
                if (/*![[wallettype_hash objectForKey:coin_walletType] boolValue] ||*/
                    ![[wallettype_hash objectForKey:back_walletType] boolValue]){
                    enableDeposit = NO;
                    enableWithdraw = NO;
                }
            }else{
                enableDeposit = NO;
                enableWithdraw = NO;
            }
            
            //  for openledger fields, only check backing coin.
            if ([backingCoinItem objectForKey:@"allow_deposit"] && ![[backingCoinItem objectForKey:@"allow_deposit"] boolValue]){
                enableDeposit = NO;
            }
            if ([backingCoinItem objectForKey:@"allow_withdrawal"] && ![[backingCoinItem objectForKey:@"allow_withdrawal"] boolValue]){
                enableWithdraw = NO;
            }
            
            //  TODO:wallet for openledger wrong backingCoinType
            id backingCoinWalletSymbol = [[backingCoinItem objectForKey:@"walletSymbol"] lowercaseString];
            if (![backingCoinWalletSymbol isEqualToString:backingCoinType]){
                //  TODO:openledger eosdac???eos.eosdac
//                CLS_LOG(@"incorrect backingCoinType: %@", backingCoinType);
                backingCoinType = backingCoinWalletSymbol;
            }
            
            NSString* symbol = [item[@"symbol"] uppercaseString];
            assert(symbol);
            assert(balanceHash);
            id balance_item = [balanceHash objectForKey:symbol] ?: @{@"iszero":@YES};
            
            GatewayAssetItemData* appext = [[GatewayAssetItemData alloc] init];
            appext.enableWithdraw = enableWithdraw;
            appext.enableDeposit = enableDeposit;
            appext.symbol = symbol;
            appext.backSymbol = [backingCoinItem[@"symbol"] uppercaseString];
            appext.name = item[@"name"];
            appext.intermediateAccount = item[@"intermediateAccount"];
            appext.balance = balance_item;
            appext.depositMinAmount = [NSString stringWithFormat:@"%@", item[@"minAmount"] ?: @""];
            appext.withdrawMinAmount = [NSString stringWithFormat:@"%@", backingCoinItem[@"minAmount"] ?: @""];
            appext.withdrawGateFee = [NSString stringWithFormat:@"%@", backingCoinItem[@"gateFee"] ?: @""];
            appext.supportMemo = [[item objectForKey:@"supportsOutputMemos"] boolValue];
            appext.coinType = item[@"coinType"];
            appext.backingCoinType = backingCoinType;
            appext.gdex_backingCoinItem = backingCoinItem;
            
            id new_item = [item mutableCopy];
            [new_item setObject:appext forKey:@"kAppExt"];
            [result addObject:[new_item copy]];
        }
    }
    
    return result;
}

/**
 *  (protected) ??????????????????API??????????????????????????????REMARK????????????????????????????????????
 *  ????????????json???????????????err???
 */
- (WsPromise*)requestDepositAddressCore:(id)item
                                 appext:(GatewayAssetItemData*)appext
            request_deposit_address_url:(id)request_deposit_address_url
                      full_account_data:(id)full_account_data
                                     vc:(VCBase*)vc
{
    if (!appext){
        appext = [item objectForKey:@"kAppExt"];
    }
    return [WsPromise promise:^(WsResolveHandler resolve, WsRejectHandler reject) {
        //  ??????????????????
        assert(full_account_data);
        id account_data = [full_account_data objectForKey:@"account"];
        id backingCoinType = [appext.backingCoinType lowercaseString];
        id coinType = [appext.coinType lowercaseString];
        id outputAddress = [account_data objectForKey:@"name"];
        id post_args = @{
                         @"inputCoinType":backingCoinType,
                         @"outputCoinType":coinType,
                         @"outputAddress":outputAddress
                         };
        //  ???????????????????????????URL
        NSString* final_url = request_deposit_address_url;
        if (!final_url){
            id request_deposit_address = [self.api_config_json objectForKey:@"request_deposit_address"];
            final_url = [NSString stringWithFormat:@"%@%@", self.api_config_json[@"base"], request_deposit_address];
        }
        [vc showBlockViewWithTitle:NSLocalizedString(@"kTipsBeRequesting", @"?????????...")];
        [[[OrgUtils asyncPostUrl_jsonBody:final_url args:post_args] then:(^id(id resp_data) {
            [vc hideBlockView];
            //{
            //    code = 8010010001;
            //    data = "<null>";
            //    message = "Content type 'application/x-www-form-urlencoded;charset=UTF-8' not supported";
            //}
            //{
            //    code = 8010030001;
            //    data = "<null>";
            //    message = "asset not found";
            //}
            if (!resp_data || [[resp_data objectForKey:@"code"] integerValue] != 0){
                resolve([resp_data objectForKey:@"message"] ?: NSLocalizedString(@"kVcDWErrTipsRequestDepositAddrFailed", @"???????????????????????????????????????????????????"));
            }else{
                //{
                //    comment = "";
                //    inputAddress = 1GJ27czcFM57w8J57fnRZhzB6NjwMiQyyX;
                //    inputCoinType = btc;
                //    inputMemo = "<null>";
                //    outputAddress = saya01;
                //    outputCoinType = "gdex.btc";
                //    refundAddress = "";
                //}
                id inputAddress = [resp_data objectForKey:@"inputAddress"];
                if (!inputAddress || [inputAddress isKindOfClass:[NSNull class]]){
                    resolve(NSLocalizedString(@"kVcDWErrTipsRequestDepositAddrFailed", @"???????????????????????????????????????????????????"));
                    return nil;
                }
                if (![[[resp_data objectForKey:@"inputCoinType"] lowercaseString] isEqualToString:backingCoinType]){
                    resolve([NSString stringWithFormat:NSLocalizedString(@"kVcDWErrTipsRequestDepositAddrFailed2", @"?????????????????????????????????'%@'???????????????????????????????????????"), @"inputCoinType"]);
                    return nil;
                }
                if (![[[resp_data objectForKey:@"outputCoinType"] lowercaseString] isEqualToString:coinType]){
                    resolve([NSString stringWithFormat:NSLocalizedString(@"kVcDWErrTipsRequestDepositAddrFailed2", @"?????????????????????????????????'%@'???????????????????????????????????????"), @"outputCoinType"]);
                    return nil;
                }
                //  ???????????????
                resolve(resp_data);
            }
            return nil;
        })] catch:(^id(id error) {
            [vc hideBlockView];
            resolve(NSLocalizedString(@"tip_network_error", @"?????????????????????????????????"));
            return nil;
        })];
    }];
}

- (WsPromise*)requestDepositAddress:(id)item fullAccountData:(id)fullAccountData vc:(VCBase*)vc
{
    assert(item);
    GatewayAssetItemData* appext = [item objectForKey:@"kAppExt"];
    assert(appext);
    return [self requestDepositAddressCore:item
                                    appext:appext
               request_deposit_address_url:nil
                         full_account_data:fullAccountData
                                        vc:vc];
}

/**
 *  ??????????????????????????????????????????
 */
- (WsPromise*)checkAddress:(id)item address:(NSString*)address memo:(NSString*)memo amount:(NSString*)amount
{
    //  TODO:???????????????
    
    GatewayAssetItemData* appext = [item objectForKey:@"kAppExt"];
    id walletType = [appext.gdex_backingCoinItem objectForKey:@"walletType"];
    assert(walletType);
    
    id check_address_base = [self.api_config_json objectForKey:@"check_address"];
    id final_url = [NSString stringWithFormat:@"%@%@", self.api_config_json[@"base"], [NSString stringWithFormat:check_address_base, walletType]];
    
    return [WsPromise promise:^(WsResolveHandler resolve, WsRejectHandler reject) {
        id args = @{
                    @"address":address,
                    };
        [[[OrgUtils asyncFetchUrl:final_url args:args] then:(^id(id resp_data) {
            if (resp_data && [resp_data isKindOfClass:[NSDictionary class]] && [[resp_data objectForKey:@"isValid"] boolValue]){
                resolve(@YES);
            }else{
                resolve(@NO);
            }
            return nil;
        })] catch:(^id(id error) {
            resolve(@NO);
            return nil;
        })];
    }];
}

/**
 *  (public) ?????????????????????????????????????????????????????????memo?????????
 */
- (WsPromise*)queryWithdrawIntermediateAccountAndFinalMemo:(GatewayAssetItemData*)appext
                                                   address:(NSString*)address
                                                      memo:(NSString*)memo
                                   intermediateAccountData:(NSDictionary*)intermediateAccountData
{
    //  GDEX ??????
    assert(intermediateAccountData);
    
    //  TODO:fowallet ??????????????????
    //  useFullAssetName        - ??????????????????????????????????????? ??????.??????
    //  assetWithdrawlAlias     - ???????????????????????????????????????bts????????????????????????
    NSString* assetName = appext.backSymbol;
    assert(assetName);
    NSString* final_memo;
    if (memo && ![memo isEqualToString:@""]){
        final_memo = [NSString stringWithFormat:@"%@:%@:%@", assetName, address, memo];
    }else{
        final_memo = [NSString stringWithFormat:@"%@:%@", assetName, address];
    }
    return [WsPromise resolve:@{@"intermediateAccount":appext.intermediateAccount,
                                @"finalMemo":final_memo,
                                @"intermediateAccountData":intermediateAccountData}];
}

/**
 *  ?????? - ??????json???value?????????????????????????????????
 */
- (NSString*)auxValueToNumberString:(id)json_value zero_as_nil:(BOOL)zero_as_nil
{
    NSDecimalNumber* value = [NSDecimalNumber decimalNumberWithString:[NSString stringWithFormat:@"%@", json_value]];
    if (zero_as_nil && [value compare:[NSDecimalNumber zero]] == NSOrderedSame){
        return nil;
    }
    return [NSString stringWithFormat:@"%@", value];
}

/**
 *  ?????? - ??????json???value??????????????????????????????????????????????????????????????????
 */
- (NSString*)auxMinValue:(id)json_value01 value02:(id)json_value02 zero_as_nil:(BOOL)zero_as_nil
{
    NSDecimalNumber* value01 = [NSDecimalNumber decimalNumberWithString:[NSString stringWithFormat:@"%@", json_value01]];
    NSDecimalNumber* value02 = [NSDecimalNumber decimalNumberWithString:[NSString stringWithFormat:@"%@", json_value02]];
    NSDecimalNumber* minValue = [value01 compare:value02] <= 0 ? value01 : value02;
    if (zero_as_nil && [minValue compare:[NSDecimalNumber zero]] == NSOrderedSame){
        return nil;
    }
    return [NSString stringWithFormat:@"%@", minValue];
}

@end

