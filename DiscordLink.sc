__config() -> {
    'scope' -> 'global',
    'bot' -> _() -> (
        global_config = read_file('config','JSON');
        if(has(global_config,'botId'), global_config:'botId', null);
    ),
    'commands' ->
    {
        'botId <id>' -> ['changeConfig','botId'],
        'chatChannelId <id>' -> ['changeConfig','chatChannelId'],
        'logChannelId <id>' -> ['changeConfig','logChannelId'],
        'adminRoleId <id>' -> ['changeConfig','adminRoleId'],
        'thumbnailUrl <url>' -> ['changeConfig','thumbnailUrl'],
        'useDiscordPictureInWebhook <boolean>' -> ['changeConfig','useDiscordPicture']
    },
    'arguments' ->
    {
        'id' -> {'type' -> 'int'},
        'url' -> {'type' -> 'term'},
        'boolean' -> {'type' -> 'bool', 'suggest' -> [true,false]}
    }
};

global_chat = dc_channel_from_id(global_config:'chatChannelId');
global_thumbnail = global_config:'thumbnailUrl';
global_aRole = dc_role_from_id(global_config:'adminRoleId');
global_log = dc_channel_from_id(global_config:'logChannelId');
global_executions = 0;
global_server = global_chat~'server';

storageFile =  read_file('data','JSON');
if(has(storageFile,'discordProfiles'),
    global_discordProfile = storageFile:'discordProfiles',
    global_discordProfile = {}
);
if(has(storageFile,'disabledCommands'),
    global_dCmds = storageFile:'disabledCommands',
    global_dCmds = {}
);
if(has(storageFile,'disabledQueries'),
    global_dQuery = storageFile:'disabledQueries',
    global_dQuery = {}
);

__on_start() -> (

    if(global_config:'botId' == null, return);
    task(_() -> (
        global_chatWebhook = dc_create_webhook(global_chat,{
            'name' -> 'Julia',
            'avatar' -> global_thumbnail
        });
        //TODO: Send as embed
        if(global_chatWebhook != null,
            logger('Julia initialzed.');
            dc_send_message(global_chat,'Server Started');
            ,//else throw error
            logger('error','Error In Initializing Webhook. Stopping server now. Please restart server to fix.');
            run('stop')
        );
    ));
);

__on_close()->(
    if(global_chatWebhook!=null,
        dc_send_message(global_chat, 'Server stopped');
        dc_delete(global_chatWebhook)
    );
);


__on_discord_message(message) -> (

    if(message~'channel'~'id'!=global_chat~'id',return); //limit to chat channel only
  	if(message~'user'~'is_bot' || message~'user'==null ,return);
    
    //Check for command
    if(len(message~'readable_content') !=0 && slice(message~'readable_content',0,1) == '!',
        cmdName = split(' ',slice(message~'readable_content',1));
        call(cmdName:0,message,cmdName);
    );

    //Normal Message
    for(player('all'),

        col = dc_get_user_color(message~'user',message~'server');
        if(col==null,col='#FFFFFF');
        rContent = message~'readable_content';

        //Link grabber
        if(rContent~'https://' || rContent~'http://' || rContent~'www.',
            
            // If its a link (It might not be)
            print(_,format(str('t Link shared by %s.Click Here To Open if from a secure source.',dc_get_display_name(message~'user',message~'server')),str('@%s',rContent))),
            //Not link
            print(_,format(str('t [Discord] '),str('b%s <%s>',col,dc_get_display_name(message~'user',message~'server')))+format(str('w  %s',rContent)))
        
        );

        player = _;
        //Attachment grabber
        if(len(message~'attachments') > 0,
            for(message~'attachments',
                fileName = _ ~ 'file_name';
                url = _ ~ 'url';
                print(player,format(str('e File shared by %s.\n Name: %s \nClick for link/automatic download.',dc_get_display_name(message~'user',message~'server'),fileName),str('@%s',url)));
            );
        );
    );
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

    if(player~'player_type' != 'fake' && global_discordProfile:(player~'uuid'):'id' == null,
        random = floor(rand(10^4 - 10^3) + 10^3);
        global_discordProfile:(player~'uuid') = {
            'verification' : random,
            'username': str(player)
        };
        run(str('kick %s Please send following message in discord chat to verify: !link %s',player,random));
        return
    );

    task(_(outer(player)) -> (
        pos = _position(player);
        dim = _dim(player);
        type = player~'player_type';

        //TODO: Send as embed

        if(type == 'multiplayer' || type == 'singleplayer' || type == 'lan_host' || type == 'lan_player',
            dc_send_message(global_chat, str('%s joined.',player)),
            type == 'fake',
            dc_send_message(global_chat, str('[BOT]%s has joined in %s at position: %s',player,dim,pos)),
            // type == 'shadow'
            dc_send_message(global_chat, str('%s shadowed in %s at position: %s',player,dim,pos)
        );
    ))
);

__on_player_disconnects(player, reason)->(

    //TODO: Send as embed
    task(_(outer(player),outer(reason)) -> (
        dc_send_message(global_chat, str('%s left. Reason:\n%s', player, reason))
    ))
);

__on_chat_message(message, player, command) -> (

    if(command,
        dc_send_message(global_log,str('%s: %s',player,message));
        return;
    );

    if(global_chatWebhook == null,
        print(player('all'),format('br Error while initializing webhook. Please ask admins for a restart.'));
        logger('Error while intializing webhook. Please restart','error');
        return;
    );

    name = str(player);
    avatar = str('https://minotar.net/helm/%s/200.png',player ~ 'name');
);

//Commands
link(message,components) -> (

    success = false;
    for(keys(global_discordProfile), 
        if(global_discordProfile:_:'verification' == components:1,

            user = message ~ 'user';
            userProfile = global_discordProfile:_;
            userProfile:'id' = user ~ 'id';
            userProfile:'name' = user ~ 'name';
            userProfile:'nickname' = dc_get_display_name(user,message~'server');
            userProfile:'avatar' = user ~ 'avatar';
            userProfile:'mention_tag' = user ~ 'mention_tag';
            userProfile:'discriminated_name' = user ~ 'discriminated_name';
            delete(userProfile:'verification');
            success = true;
            break;
        );
    );
    //TODO: Send as embed
    task(_(outer(message)) -> (
        if(success,
            dc_send_message(global_chat,{
                'content' -> str('Your account has been linked to %s',userProfile:'username'),
                'reply_to' -> message
            });
        )
    ));
);

// Helper Functions

_dim(player) -> (
    dimension = player~'dimension';
    if(dimension == 'overworld', return('Overworld'),
       dimension == 'the_nether', return('Nether'),
       return('End')
    )
);

_position(player) -> (
    return(map(pos(player),floor(_)))
);