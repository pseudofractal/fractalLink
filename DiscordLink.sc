__config() -> {
    'scope'->'global',
    'bot'->'BotId' //Bot Id, use developer options
};

//Change values for your server ***************************************************************************************
global_chat = dc_channel_from_id('ChannelId'); //Chat Channel Id
global_sThumbnail = 'https://imgur.com/a/9ckY1YY.jpg';//Thumbnail, please dont forget .jpg/other file formats I dont think it works without that
global_admin_role = dc_role_from_id('RoleId'); //Role Id for Console access
global_log = dc_channel_from_id('ChannelId'); //Channel for console log
//************************************************************************************************************


//No need to look below here***************************************************************************************************
global_executions = 0;
global_server = global_chat~'server';

// Settings and IdFile creation
idFile= read_file('DiscordId','json');
if(idFile==null,
	//when no file found
    global_idData={};
    ,//when file found
    global_idData=idFile;
);
qFile = read_file('query','json');
if(qFile==null,
    //when no file is found
    global_qSetting=[];
    , //when file is found
    global_qSetting = qFile;
);
cFile = read_file('commands','json');
if(cFile==null,
    //when no file is found
    global_dCmd=[];
    ,//when file is found
    global_dCmd = cFile;
);


//Event Functions
__on_server_starts()-> (
    task(_()->(
        global_chat_webhook= dc_create_webhook(global_chat,{
            'name' -> 'Init',
	        'avatar' -> 'https://raw.githubusercontent.com/replaceitem/carpet-discarpet/master/src/main/resources/assets/discarpet/icon.png'
        });
        if(global_chat_webhook!=null,
            logger('FractalLink initialzed.');
            dc_send_message(global_chat,'Server Started')
        ,
            logger('error','Error In Initializing Webhook. Please restart server to fix.')
        );
    ));
);

__on_server_shuts_down()-> (
    if(global_chat_webhook==null,return());
    dc_delete_webhook(global_chat_webhook);
    dc_send_message(global_chat, 'Server stopped');
);

__on_tick() -> (
    global_executions = 0;
);

__on_discord_message(message) -> (

	if(message~'channel'~'id'!=global_chat~'id',return()); //limit to chat channel only
  	if(message~'user'~'is_bot' || message~'user'==null ,return());
    _cmdCheck(message, message~'channel');
  	_chatLink(message)
    
);

__on_system_message(text,type,entity) -> (
    global_executions += 1; //prevent recursion
    if(global_executions < 10,
        if((type~'commands.save.') == null, //dont send 'saving world' messages
            task(_(outer(text)) -> (
                dc_send_message(global_log,text); //send to discord
            ));
        );
    );
);

__on_player_connects(player) -> (

	if(player~'player_type'=='fake',
    	global_fake_player+=player;
        x=floor(pos(player):0);        
        y=floor(pos(player):1); 
        z=floor(pos(player):2);
        task(_(outer(player),outer(x),outer(y),outer(z))->(
            dc_send_message(global_chat,str('[Bot] %s joined at %s in %s',player,[x,y,z],query(player,'dimension')));
        ));
    );  
    if(player~'player_type'=='multiplayer',
    	task(_(outer(player))->(
            dc_send_message(global_chat, str('%s joined',player));
        ));
    );

);

__on_player_disconnects(player, reason)->(
	if(player~'player_type'=='fake',
    	delete(global_fake_player,player)
      );
   	task(_(outer(player))->(
           dc_send_message(global_chat, str('%s left',player));
       ));
);

__on_chat_message(message, player, command) -> {
    if(command,return());

    if(global_chat_webhook==null,
        print(player('all'),format('br Error while initializing webhook. Please ask admins for a restart.'));
        logger('Error whule intializing webhook. Pleasre restart','error');
        return();
    );

    dName = str(player);
    dAvatar = str('https://minotar.net/helm/%s/200.png',player ~ 'name');
    msg = message;
    if(message~'@'!=null,
        parts= split(' ',message);
        for(parts,
            if(_~'@',
                name = split('@',_):1;
                for(global_chat~'server'~'users',
                    print(player('all'),dc_get_display_name(_,global_chat~'server'));
                    print(player('all'),name);
                    if(dc_get_display_name(_,global_chat~'server')==name,
                        print(player('all'),dc_get_display_name(_,global_chat~'server'));
                        msg=replace(message,'@'+name,_~'mention_tag');
                    );
                );
            )
    ));

	task(_(outer(dName),outer(dAvatar),outer(msg))->(dc_send_webhook(global_chat_webhook, msg, {'name' -> dName, 'avatar' -> dAvatar});));

	};
    
//Utility functions

_cmdCheck(message, channel) -> (
	
    text= message~'readable_content';
    if(length(text)==0,return());
    char = slice(text,0,1);
    
	if(char=='!',		
        cmd = split(' ',slice(text,1));
        
        
        if(cmd:0=='status' && global_dCmd~'status'==null,
        	
            mspt=ceil(system_info('server_last_tick_times'):0);
            tps= 1000/mspt;
            if((1000/mspt)>20, tps=str(20);
            
            tps=str(tps);
            mspt=str(mspt);
            
            playerList= _playerList('multiplayer');
            botList= _playerList('fake');
            
            playerName='';
            botName='';
            for(playerList,	
                playerName+= str('%s\n',_);
            );
            if(length(playerList)==0, playerName='No players online.');
            
            for(botList,
            	botName+= str('%s\n',_);
            );
            if(length(botList)==0, botName='No bots online.');
            
            playerCount = str(length(player('all')));
            requester= str('Requested By: %s', message~'user'~'name');
            requesterProfile= str(message~'user'~'avatar');
            
            f = [
    	{
        	'name' -> 'TPS',
            'value' -> tps,
            'inline' -> true
        },
        {
        	'name' -> 'MSPT',
            'value' -> mspt,
            'inline' -> true
        },
        {
            'name'->'Player Count',
            'value'-> playerCount
        },
        {
            'name'->'Players',
            'value'-> playerName
        },
        {
        	'name' -> 'Bots',
            'value' -> botName
        }
        ];
    
		e=_makeEmbed('Status','Server is Online',f,requester,requesterProfile,global_sThumbnail);	
        
        task(_(outer(e),outer(message),outer(channel))->(
                dc_send_message(channel,{
            	'content' -> '',
                'embeds' -> [e],
                'reply_to' -> message
            })    
            ));
            
        ));
        
        
        if(cmd:0=='link' && global_dCmd~'link'==null,
        	
            if(global_idData~str(cmd:1),
            	
                dc_send_message(channel, str('%s is already linked to another account. Please ask Surf/Mac /owner of original account to unlink.',cmd:1));
                return();
            
            );
        	
            usr=message~'user'~'id';
			global_idData:str(cmd:1)=usr;
            delete_file('DiscordId','json');
            write_file('DiscordId','json',global_idData);    
			dc_send_message(channel,{
                'content' -> str('%s has successfully been linked to your account.',cmd:1),
                'reply_to' -> message
            });
            
        );       
        
        if(cmd:0=='unlink' && global_dCmd~'unlink'==null,
        	
            if(!global_idData~str(cmd:1),
            	dc_send_message(channel,str('%s is not linked to any profile.',cmd:1)),
            
            //else if
            global_idData:str(cmd:1)!=str(message~'user'~'id'),
            	dc_send_message(channel,str('%s is not your account. Cease trolling.',cmd:1)),
                
            //else    
          	global_idData:str(cmd:1)==str(message~'user'~'id'),
               	delete(global_idData,str(cmd:1));
                delete_file('DiscordId','json');
                write_file('DiscordId','json',global_idData);
                dc_send_message(channel,{
                    'content' -> str('%s has been unlinked from your account, Please relink to correct one.',cmd:1),
                    'reply_to' -> message
                });
            );
        
        );
        
        if(cmd:0=='query' && global_dCmd~'query'==null,
        
           	p=null;
			for(player('all'),
            	if(_~'name'==str(cmd:1),
                	p=_;
   					break();
                );      
            );
            
            if(p==null,  	
                dc_send_message(channel,'Requested User Is Not Online');
                return();
            );
            requester= str('Requested By: %s', message~'user'~'name');
            requesterProfile= str(message~'user'~'avatar');
            
            position= pos(p);
            position:0=floor(position:0);
            position:1=floor(position:1);
            position:2=floor(position:2);
            effectList = '';
            for(p~'effect',
            	effectList+= str(' %s %s\n',_:0,_:1);
            );
            
            if(length(p~'effect')==0,
            	effectList= 'No active effects';
            );
            
            if(global_idData:str(p)==null,
            	name = str('Account not linked');
                thumbnail = str('https://mc-heads.net/avatar/%s',p~'name');
                ,
                
                //else
                discordProfile= dc_user_from_id(global_idData:str(p));
                name= discordProfile ~ 'discriminated_name';
				thumbnail= discordProfile ~ 'avatar';
            );    
            m=query(p,'holds','mainhand'):0;
            o=query(p,'holds','offhand'):0;
            h=query(p,'holds','head'):0;
            c=query(p,'holds','chest'):0;
            l=query(p,'holds','legs'):0;
            f=query(p,'holds','feet'):0;

            m= replace(m,'_',' ');
            o= replace(o,'_',' ');
            h= replace(h,'_',' ');
            c= replace(c,'_',' ');
            l= replace(l,'_',' ');
            f= replace(f,'_',' ');

            if(m=='null', m='');
            if(o=='null', o='');
            if(h=='null', h='');
            if(c=='null', c='');
            if(l=='null', l='');
            if(f=='null', f='');
            
            holds= str('Mainhand: %s\nOffhand: %s\nHelmet: %s\nChestplate: %s\nLeggings: %s\nBoots: %s',m,o,h,c,l,f);

            f = [
            {
        	'name' -> 'Health',
            'value' -> str(p~'health'),
            'inline' -> true
            },
            {
        	'name' -> 'Ping',
            'value' -> str(p~'ping'),
            }
            ];

            if(global_qSetting~'food'==null,
                f += {
                    'name'->'Food Status',
                    'value'-> str('Hunger: %s \nSaturation:%s',p~'hunger',p~'saturation')
                    }
            );

            if(global_qSetting~'position' ==null,
                f += [
                    {
        	            'name' -> 'Dimension',
                        'value' -> str(p~'dimension'),
                        'inline' -> true
                    },
                    {
                        'name'->'Position',
                        'value'-> str(position),
                        'inline' -> true
                    }
                    ]
            );

            if(global_qSetting~'activity'==null,
                f += {
        	        'name' -> 'Activity',
                    'value' -> str(p~'pose')
                    }
            );

            if(global_qSetting~'effects'==null,
                f += {
        	        'name' -> 'Active Effects',
                    'value' -> effectList
                }
            );

            if(global_qSetting~'items'==null,
                f += {
        	        'name' -> 'Items',
                    'value' -> holds
                }
            );

            if(global_qSetting~'discord'==null,
                f += {
            	'name' -> 'Discord Profile',
                'value' -> name
                }
            );


            e=_makeEmbed('Query',str(p),f,requester,requesterProfile,thumbnail);
            task(_(outer(e),outer(message),outer(channel))->(
                dc_send_message(channel,{
            	'content' -> '',
                'embeds' -> [e],
                'reply_to' -> message
            })    
            ));

            );

        if(cmd:0=='console'  && global_dCmd~'console'==null,

            admin = false;
            for(dc_get_user_roles(message~'user',global_server),
                if(_~'id'==global_admin_role~'id',
                    admin=true;
                );
            );
            if(!admin,
                dc_send_message(channel,{
                    'content' -> str('You dont have the necessary permissions for executing command'),
                    'reply_to' -> message
                });
                return();
            );
            cd = split('!console ',text);
            result = run(cd:1);
            if(result:0==0 && result:2!=null,
            dc_send_message(channel,{
                'content' -> str('Error: %s',str(result:2)),
                'reply_to' -> message
            });
            );
            if(result:0==1,
                dc_send_message(channel,{
                    'content' -> str('Success: %s',str(result:1)),
                    'reply_to' -> message
                });
            );

        );

        if(cmd:0=='hardware'  && global_dCmd~'hardware'==null,

            requester= str('Requested By: %s', message~'user'~'name');
            requesterProfile= str(message~'user'~'avatar');
            thumbnail = global_sThumbnail;

            max_mem = system_info('java_max_memory');
            alloc_mem = system_info('java_allocated_memory');
            used_mem = system_info('java_used_memory');
            cpu_count = system_info('java_cpu_count');
            system_cpu = system_info('java_system_cpu_load');
            jvm_cpu = system_info('java_process_cpu_load');
            
            memoryStuff = str('Maximum allowed memory accessible by JVM: %s \nCurrently allocated memory by JVM: %s \nCurrently used memory by JVM: %s',mex_mem,alloc_mem,used_mem);
            cpuStuff = str('Number of processors: %s \nCurrent percentage of CPU used by the system: %s \nCurrent percentage of CPU used by JVM: %s',cpu_count,system_cpu,jvm_cpu);
            
            f = [
                {
                    'name' -> 'Memory Stuff',
                    'value' -> memoryStuff
                },
                {
                    'name' -> 'Cpu Stuff',
                    'value' -> cpuStuff
                }
                ];
            
            e =_makeEmbed('Hardware Property','Usage:',f,requester,requesterProfile,thumbnail);
            task(_(outer(e),outer(message),outer(channel))->(
                dc_send_message(channel,{
            	'content' -> '',
                'embeds' -> [e],
                'reply_to' -> message
            })    
            ));
        );

        if(cmd:0=='help',

            requester= str('Requested By: %s', message~'user'~'name');
            requesterProfile= str(message~'user'~'avatar');
            thumbnail = global_sThumbnail;

            f=[
                {
                    'name' -> 'status',
                    'value' -> 'Tells about performance and player count.'
                },
                {
                    'name' -> 'query',
                    'value' -> 'Allows you to query an online player and know more about its position,etc. \nUsage: !query [player name]'
                },
                {
                    'name' -> 'pinv',
                    'value' -> 'Query the entire player inventory. \nUsage: !pinv [player name]'
                },
                {
                    'name' -> 'inv',
                    'value' -> 'Query the contents of an inventory. \nUsage: !inv [x coordinate] [y coordinate] [z coordinate]'
                },
                {
                    'name' -> 'deepinv',
                    'value' -> 'Tells more about an item in a particular inventory slot.\nUsage: !deepinv [x coordinate] [y coordinate] [z coordinate] [slot number]'
                },
                {
                    'name' -> 'link',
                    'value' -> 'Used to link your minecraft account to discord account. \nWill serve purpose in future plans. \nUsage !link [minecraft name]'
                },
                {
                    'name' -> 'unlink',
                    'value' -> 'Self explanatory. Used to unlink'
                },
                {
                    'name' -> 'console',
                    'value' -> 'Run commands on console, need admin rule to be specefied. \nUsage: !console [command(without /)]'
                },
                {
                    'name' -> 'hardware',
                    'value' -> 'Provides information about cpu usage,ram usage etc'
                },
                {
                    'name' -> 'dcmd',
                    'value' -> 'Add/Remove/List all disabled utility commands. \nUsage: !dcmd [add/remove] [The coorect command name from help] \nUsage: !dcmd list ;(will show list of disabled commands)'
                },
                {
                    'name' -> 'dquery',
                    'value' -> 'Add/Remove/List all disabled query features. Same usages as dcmd'
                }
            ];

            e =_makeEmbed('Help','Available Commands:',f,requester,requesterProfile,thumbnail);
            task(_(outer(e),outer(message),outer(channel))->(
                dc_send_message(channel,{
            	'content' -> '',
                'embeds' -> [e],
                'reply_to' -> message
            })    
            ));

        );

        if(cmd:0=='pinv' && global_dCmd~'pinv'==null,
        
            p=null;
			for(player('all'),
            	if(_~'name'==str(cmd:1),
                	p=_;
   					break();
                );      
            );
            
            if(p==null,  	
                dc_send_message(channel,'Requested User Is Not Online');
                return();
            );
            requester= str('Requested By: %s', message~'user'~'name');
            requesterProfile= str(message~'user'~'avatar');
            thumbnail = str('https://mc-heads.net/avatar/%s',p~'name');

            iString='';
            c_for(i=0,i<inventory_size(p),i+=1,

                item = inventory_get(p,i);
                if(item != null,
                    itemName = inventory_get(p,i):0;
                    itemCount = inventory_get(p,i):1;
                    //String Manipulation Bullcrap
                    itemName = replace(itemName,'_',' ');
                    firstLetter = slice(itemName,0,1);
                    itemName = replace_first(itemName,firstLetter,upper(firstLetter));
                    //
                    iString += str('Slot %d: %s  ,Count: %d\n',i,itemName,itemCount);
                );  
                
            );

            f = [
                {
                    'name' -> 'Player Inventory',
                    'value' -> iString
                }
            ];

            e =_makeEmbed(str(p),'',f,requester,requesterProfile,thumbnail);
            task(_(outer(e),outer(message),outer(channel))->(
                dc_send_message(channel,{
            	'content' -> '',
                'embeds' -> [e],
                'reply_to' -> message
            })    
            ));

        );

        if(cmd:0 == 'inv' && global_dCmd~'inv'==null,

            x = number(cmd:1);
            y = number(cmd:2);
            z = number(cmd:3);
            
            if(inventory_get(block(x,y,z))==null,
                dc_send_message(message~'channel',{
                    'content' -> 'No inventory exists at that position.',
                    'reply_to' -> message
                });
                return();
            );

            storageMap= {};
            for(inventory_get(block(x,y,z)),

                    if(_==null, continue());
                    itemName = _:0;
                //String Manipulation Stuff
                    itemName = replace(itemName,'_',' ');
                    firstLetter = slice(itemName,0,1);
                    itemName = replace_first(itemName,firstLetter,upper(firstLetter));
                //
                    itemCount = _:1;
                    if(storageMap:str(itemName)==null,storageMap:str(itemName)={});
                    itemCount = storageMap:str(itemName):str('count') + itemCount;
                    storageMap:str(itemName):str('count') = itemCount;              
            );

            storageString = '';
            for(keys(storageMap),
                storageString += str('%s, Count:%s\n',_,storageMap:str(_):'count');
            );

            requester= str('Requested By: %s', message~'user'~'name');
            requesterProfile= str(message~'user'~'avatar');
            thumbnail = global_sThumbnail;
            
            f = [
                {
                    'name' -> str('Inventory Contents'),
                    'value' -> storageString
                }
            ];

            e =_makeEmbed(str(block(x,y,z)),'',f,requester,requesterProfile,thumbnail);
            task(_(outer(e),outer(message),outer(channel))->(
                dc_send_message(channel,{
            	'content' -> '',
                'embeds' -> [e],
                'reply_to' -> message
            })    
            ));
    );

    if(cmd:0 == 'deepinv'  && global_dCmd~'deepinv'==null,

        x = number(cmd:1);
        y = number(cmd:2);
        z = number(cmd:3);
        slot = number(cmd:4)-1;

        item = inventory_get(block(x,y,z),slot);
        itemName = item:0;
        //String Manipulation Stuff
            itemName = replace(itemName,'_',' ');
            firstLetter = slice(itemName,0,1);
            itemName = replace_first(itemName,firstLetter,upper(firstLetter));
        //
        itemCount = item:1;
        itemNbt = item:2;

        if(itemNbt==null, itemNbt='No nbt data');

        requester= str('Requested By: %s', message~'user'~'name');
        requesterProfile= str(message~'user'~'avatar');
        thumbnail = global_sThumbnail;

        if(!itemName~'box',
        f = [
            {
                'name' -> 'Name',
                'value' -> str(itemName),
                'inline' -> true
            },
            {
                'name' -> 'Count',
                'value' -> str(itemCount),
                'inline' -> true
            },
            {
                'name' -> 'Nbt Data',
                'value' -> str(itemNbt)
            }
            ],
            // When shulker box

            print(player('all'),type(itemNbt:'BlockEntityTag':'Items'));
            content = parse_nbt(itemNbt:'BlockEntityTag':'Items');
            contentMap={};

            for(content,
                //String manipulation bullcrap
                name =  _:'id';
                name = replace(name,'minecraft:','');
                name = replace(name,'_','');
                firstLetter = slice(name,0,1);
                name = replace_first(name,firstLetter,upper(firstLetter));
                // Ends
                if(contentMap:str(name)==null, contentMap:str(name)={});
                contentMap:str(name):'count' += _:'Count';
            );
            contentString = '';
            for(keys(contentMap),
                contentString += str('Name: %s, Count:%s\n',_,contentMap:str(_):'count');
            );

            f = [
                {
                    'name' -> 'Name',
                    'value' -> str(itemName),
                    'inline' -> true
                },
                {
                    'name' -> 'Count',
                    'value' -> str(itemCount),
                    'inline' -> true
                },
                {
                    'name' -> 'Contents',
                    'value' -> contentString
                }
                ]
            
            );

        e =_makeEmbed(str(block(x,y,z)),str('Slot: %d',slot+1),f,requester,requesterProfile,thumbnail);
            task(_(outer(e),outer(message),outer(channel))->(
                dc_send_message(channel,{
            	'content' -> '',
                'embeds' -> [e],
                'reply_to' -> message
            })    
            ));
        
    );

    if(cmd:0 == 'dcmd',

        requester= str('Requested By: %s', message~'user'~'name');
        requesterProfile= str(message~'user'~'avatar');
        thumbnail = global_sThumbnail;
        a = '';
        if(cmd:1 == 'list',
            a = 'Subcommand used: List';
            dCmd = '';
            for(global_dCmd, dCmd+=str('%s \n',_));
            f=[{
                'name' -> 'Disabled Commands',
                'value' -> dCmd
                }
            ];
        );

        if(cmd:1 == 'add',
            a = 'Subcommand used: Add';
            global_dCmd+= cmd:2;
            delete_file('commands','json');
            write_file('commands','json',global_dCmd);
            dCmd = '';
            for(global_dCmd, dCmd+=str('%s \n',_));
            f=[{
                'name' -> 'Updated Command List',
                'value' -> dCmd
                }
            ];
        );

        if(cmd:1 == 'remove',
            a = 'Subcommand used: Remove';
            delete(global_dCmd,cmd:2);
            delete_file('commands','json');
            write_file('commands','json',global_dCmd);
            dCmd = '';
            for(global_dCmd, dCmd+=str('%s \n',_));
            f=[{
                'name' -> 'Updated Command List',
                'value' -> dCmd
                }
            ];
        );


        e =_makeEmbed('Disabled Utility Commands',a,f,requester,requesterProfile,thumbnail);
            task(_(outer(e),outer(message),outer(channel))->(
                dc_send_message(channel,{
            	'content' -> '',
                'embeds' -> [e],
                'reply_to' -> message
            })    
            ));

    );

    if(cmd:0 == 'dquery',

        requester= str('Requested By: %s', message~'user'~'name');
        requesterProfile= str(message~'user'~'avatar');
        thumbnail = global_sThumbnail;
        a = '';

        if(cmd:1 == 'list',
            a = 'Subcommand used: List';
            dQ = '';
            for(global_qSetting, dQ += str('%s \n',_));
            f = [{
                'name' -> 'Disabled Query Fields',
                'value' -> dQ
            }];
        );

        if(cmd:1 == 'add',
            a = 'Subcommand used: Add';
            global_qSetting += cmd:2;
            delete_file('query','json');
            write_file('query','json',global_qSetting);
            dQ = '';
            for(global_qSetting, dQ += str('%s \n',_));
            f=[{
                'name' -> 'Updated disabled queries',
                'value' -> dQ
            }];          
        );

        if(cmd:1 =='remove',
            a = 'Subcommand used: Remove';
            delete(global_qSetting,cmd:2);
            delete_file('query','json');
            write_file('query','json',global_qSetting);
            dQ = '';
            for(global_qSetting, dQ += str('%s \n',_));
            f=[{
                'name' -> 'Updated disabled query list',
                'value' -> dQ
            }];
        );

        if(cmd:1 == 'help',
            a = '';
            f=[{
                'name' -> 'List of available queries',
                'value' -> 'food\nposition\nactivity\neffects\nitems'
            }]
        );
    
    );
));

_chatLink(message)->(

	for(player('all'),
        col = 'd'; // this could be replaced with a custom way of fetching user color
        if(col == null,col = 'w');
        col = dc_get_user_color(message~'user',message~'server');
        if(col==null,col='#FFFFFF');
        rContent= message~'readable_content';
        if(rContent=='', rContent = 'No message');
        link=false;
        if(rContent~'https://' || rContent~'http://', link=true);
        print(_,format(str('t [Discord] '),str('b%s <%s>',col,dc_get_display_name(message~'user',message~'server')))+format(str('w  %s',message~'readable_content')));
        if(link, print(_,format('t Link shared.Click Here To Open.',str('@%s',rContent)));); //Surf notes: grEen,Cyan,Teal (Light bloo)
        
        attachments = message~'attachments';
        if(length(attachments)>0,
            c_for(i = 0, i<length(attachments), i+=1,
                print(_,i);
                attach = get(attachments,i);
                fileName = attach~'file_name';
                url = attach ~ 'url';
                print(_,format(str('e File shared.\n Name: %s \nCLick for link/automatic download.',fileName),str('@%s',url)))
            );
        );

    );

);

_playerList(category) -> (
	List=[];
    for(player('all'),
    	if(query(_,'player_type')==category, List+=_)
    );
    return(List);
);

_makeEmbed(t,d,f,requester, requesterProfile, thumb) -> (

	e = {    
    'title'-> str(t),
    'description'-> str(d),
    'fields'->f,
    'color'->[0,255,220],
    'footer'->{
        'text'-> requester,
        'icon'-> requesterProfile
    },
    'thumbnail'->thumb
    };
    return(e);

);
