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
if(has(storageFile,'discordId'),
    global_discordId = storageFile:'discordId',
    global_discordId = {}
);
if(has(storageFile,'disabledCommands'),
    global_dCmds = storageFile:'disabledCommands',
    global_dCmds = {}
);
if(has(storageFile,'disabledQuery'),
    global_dQuery = storageFile:'disabledQuery',
    global_dQuery = {}
);

__on_start() -> (

    if(global_config:'botId' == null, return);
    task(_() -> (
        global_chatWebhook = dc_create_webhook(global_chat,{
            'name' -> 'Julia',
            'avatar' -> global_thumbnail
        });
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
        call(cmdName,message);
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
    task(_(outer(player)) -> (
        pos = _position(player);
        dim = _dim(player);
        type = player~'player_type';

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